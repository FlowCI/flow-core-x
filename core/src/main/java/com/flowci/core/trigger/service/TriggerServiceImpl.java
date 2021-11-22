package com.flowci.core.trigger.service;

import com.flowci.core.agent.event.AgentStatusEvent;
import com.flowci.core.common.domain.Mongoable;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.manager.ConditionManager;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.config.domain.Config;
import com.flowci.core.config.domain.SmtpConfig;
import com.flowci.core.config.service.ConfigService;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.job.event.JobFinishedEvent;
import com.flowci.core.trigger.dao.TriggerDao;
import com.flowci.core.trigger.domain.EmailTrigger;
import com.flowci.core.trigger.domain.Trigger;
import com.flowci.core.trigger.domain.WebhookTrigger;
import com.flowci.core.trigger.event.EmailTemplateParsedEvent;
import com.flowci.domain.Vars;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.util.StringHelper;
import groovy.util.ScriptException;
import lombok.extern.log4j.Log4j2;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Log4j2
@Service
public class TriggerServiceImpl implements TriggerService {

    private final TemplateEngine templateEngine;

    private final TriggerDao triggerDao;

    private final ConditionManager conditionManager;

    private final SessionManager sessionManager;

    private final SpringEventManager eventManager;

    private final ThreadPoolTaskExecutor appTaskExecutor;

    private final ConfigService configService;

    private final FlowService flowService;

    private final String emailTemplate;

    private final HttpClient httpClient;

    public TriggerServiceImpl(TemplateEngine templateEngine,
                              TriggerDao triggerDao,
                              ConditionManager conditionManager,
                              SessionManager sessionManager,
                              SpringEventManager eventManager,
                              ThreadPoolTaskExecutor appTaskExecutor,
                              ConfigService configService,
                              FlowService flowService) throws IOException {
        this.templateEngine = templateEngine;
        this.templateEngine.setTemplateResolver(new StringTemplateResolver());

        this.triggerDao = triggerDao;
        this.conditionManager = conditionManager;
        this.sessionManager = sessionManager;
        this.eventManager = eventManager;
        this.appTaskExecutor = appTaskExecutor;
        this.configService = configService;
        this.flowService = flowService;

        Resource resource = new ClassPathResource("templates/email.html");
        this.emailTemplate = new String(Files.readAllBytes(resource.getFile().toPath()));

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public List<Trigger> list() {
        return triggerDao.findAll(Mongoable.SortByCreatedAtASC);
    }

    @Override
    public Trigger get(String id) {
        Optional<Trigger> optional = triggerDao.findById(id);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("Trigger {0} is not found", id);
    }

    @Override
    public Trigger getByName(String name) {
        Optional<Trigger> optional = triggerDao.findByName(name);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("Trigger {0} is not found", name);
    }

    @Override
    public Trigger save(EmailTrigger e) {
        Config config = configService.get(e.getSmtpConfig());
        if (config.getCategory() != Config.Category.SMTP) {
            throw new StatusException("SMTP config is required");
        }

        validateCondition(e);
        e.setCreatedAndUpdatedBy(sessionManager.getUserEmail());
        return doSave(e);
    }

    @Override
    public Trigger save(WebhookTrigger w) {
        return doSave(w);
    }

    @Override
    public void send(Trigger t, Vars<String> context) {
        if (t.hasCondition()) {
            try {
                conditionManager.run(t.getCondition(), context);
            } catch (ScriptException e) {
                log.warn("Cannot execute condition of trigger {}", t.getName());
                return;
            }
        }

        var error = StringHelper.EMPTY;
        try {
            if (t instanceof EmailTrigger) {
                doSend((EmailTrigger) t, context);
                return;
            }

            if (t instanceof WebhookTrigger) {
                doSend((WebhookTrigger) t, context);
            }
        } catch (Exception e) {
            log.warn("Error on trigger {}", t.getName(), e);
            error = e.getMessage();
        } finally {
            t.setError(error);
            triggerDao.save(t);
        }
    }

    @Override
    public Trigger delete(String name) {
        Trigger n = getByName(name);
        triggerDao.deleteByName(name);
        return n;
    }

    @EventListener
    public void onJobStatusChange(JobFinishedEvent event) {
        Vars<String> context = event.getJob().getContext();
        List<Trigger> list = triggerDao.findAllByAction(Trigger.Action.OnJobFinished);
        for (Trigger n : list) {
            appTaskExecutor.execute(() -> send(n, context));
        }
    }

    @EventListener
    public void onAgentStatusChange(AgentStatusEvent event) {
        Vars<String> context = event.getAgent().toContext();
        List<Trigger> list = triggerDao.findAllByAction(Trigger.Action.OnAgentStatusChange);
        for (Trigger n : list) {
            appTaskExecutor.execute(() -> send(n, context));
        }
    }

    private Trigger doSave(Trigger t) {
        try {
            return triggerDao.save(t);
        } catch (DuplicateKeyException ignore) {
            throw new DuplicateException("Trigger name {0} is already defined", t.getName());
        }
    }

    private void validateCondition(Trigger n) {
        if (!n.hasCondition()) {
            return;
        }

        try {
            conditionManager.verify(n.getCondition());
        } catch (ScriptException ex) {
            throw new StatusException("Invalid groovy condition: " + ex.getMessage());
        }
    }

    private void doSend(EmailTrigger t, Vars<String> context) throws MessagingException {
        JavaMailSender sender = configService.getEmailSender(t.getSmtpConfig());
        IContext thymeleafContext = toThymeleafContext(context);
        String htmlContent = templateEngine.process(emailTemplate, thymeleafContext);
        eventManager.publish(new EmailTemplateParsedEvent(this, htmlContent));

        MimeMessage mime = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, false);

        String from = t.getFrom();
        String[] to = {t.getTo()};

        if (!t.hasFrom()) {
            SmtpConfig config = (SmtpConfig) configService.get(t.getSmtpConfig());
            from = config.getAuth().getUsername();
        }

        // load all users from flow
        if (t.isToFlowUsers()) {
            String flow = context.get(Variables.Flow.Name);
            if (!StringHelper.hasValue(flow)) {
                throw new StatusException("flow name is missing from context");
            }

            List<String> users = flowService.listUsers(flow);
            to = users.toArray(new String[0]);
        }

        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(templateEngine.process(t.getSubject(), thymeleafContext));
        helper.setText(htmlContent, true);

        sender.send(mime);
        log.debug("Email trigger {} has been sent", t.getName());
    }

    private void doSend(WebhookTrigger t, Vars<String> context) throws Exception {
        var uriBuilder = new URIBuilder(t.getUrl());
        if (t.hasParams()) {
            t.getParams().forEach(uriBuilder::addParameter);
        }

        var builder = HttpRequest.newBuilder(uriBuilder.build());
        if (t.hasHeaders()) {
            t.getHeaders().forEach(builder::setHeader);
        }

        var bodyPublisher = HttpRequest.BodyPublishers.noBody();
        if (t.hasBody() && !Objects.equals(t.getHttpMethod(), "GET")) {
            IContext thymeleafContext = toThymeleafContext(context);
            bodyPublisher = HttpRequest.BodyPublishers.ofString(templateEngine.process(t.getBody(), thymeleafContext));
        }
        builder = builder.method(t.getHttpMethod(), bodyPublisher);

        var response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        log.debug("Webhook trigger {} has been sent: status code = {}, body = {}", t.getName(), response.statusCode(), response.body());
    }

    private IContext toThymeleafContext(Vars<String> c) {
        Context context = new Context();
        c.forEach(context::setVariable);
        return context;
    }
}
