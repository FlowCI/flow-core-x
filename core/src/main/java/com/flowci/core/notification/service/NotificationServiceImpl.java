package com.flowci.core.notification.service;

import com.flowci.core.agent.event.AgentStatusEvent;
import com.flowci.core.common.domain.Mongoable;
import com.flowci.core.common.manager.ConditionManager;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.config.domain.Config;
import com.flowci.core.config.event.GetConfigEvent;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.notification.dao.NotificationDao;
import com.flowci.core.notification.domain.EmailNotification;
import com.flowci.core.notification.domain.Notification;
import com.flowci.core.notification.domain.WebhookNotification;
import com.flowci.domain.Vars;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.util.StringHelper;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import groovy.util.ScriptException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class NotificationServiceImpl implements NotificationService {

    private final Configuration templateCfg = new Configuration(Configuration.VERSION_2_3_29);

    private final StringTemplateLoader templateLoader = new StringTemplateLoader();

    @Autowired
    private NotificationDao notificationDao;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private ConditionManager conditionManager;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private ThreadPoolTaskExecutor appTaskExecutor;

    @PostConstruct
    public void init() {
        templateCfg.setTemplateLoader(templateLoader);
        templateCfg.setLogTemplateExceptions(false);
    }

    @Override
    public List<Notification> list() {
        return notificationDao.findAll(Mongoable.SortByCreatedAtASC);
    }

    @Override
    public Notification get(String id) {
        Optional<Notification> optional = notificationDao.findById(id);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("Notification {0} is not found", id);
    }

    @Override
    public void save(EmailNotification e) {
        GetConfigEvent event = eventManager.publish(new GetConfigEvent(this, e.getSmtpConfig()));
        if (event.hasError()) {
            throw new StatusException("Invalid config name");
        }

        Config config = event.getFetched();
        if (config.getCategory() != Config.Category.SMTP) {
            throw new StatusException("SMTP config is required");
        }

        validateCondition(e);
        validateTemplate(e);

        e.setCreatedAndUpdatedBy(sessionManager.getUserEmail());
        notificationDao.save(e);
    }

    @Override
    public void save(WebhookNotification w) {
        notificationDao.save(w);
    }

    @Override
    public void delete(String id) {
        notificationDao.deleteById(id);
    }

    @Override
    public void send(Notification n, Vars<String> context) {
        if (n.hasCondition()) {
            try {
                conditionManager.run(n.getCondition(), context);
            } catch (ScriptException e) {
                log.warn("Cannot execute condition of notification {}", n.getName());
                return;
            }
        }

        if (n instanceof EmailNotification) {
            doSend((EmailNotification) n);
            return;
        }

        if (n instanceof WebhookNotification) {
            doSend((WebhookNotification) n);
        }
    }

    @EventListener
    public void onJobStatusChange(JobStatusChangeEvent event) {
        Vars<String> context = event.getJob().getContext();
        List<Notification> list = notificationDao.findAllByTrigger(Notification.TriggerAction.OnJobStatusChange);
        for (Notification n : list) {
            appTaskExecutor.execute(() -> send(n, context));
        }
    }

    @EventListener
    public void onAgentStatusChange(AgentStatusEvent event) {
        Vars<String> context = event.getAgent().toContext();
        List<Notification> list = notificationDao.findAllByTrigger(Notification.TriggerAction.OnAgentStatusChange);
        for (Notification n : list) {
            appTaskExecutor.execute(() -> send(n, context));
        }
    }

    private void validateCondition(Notification n) {
        if (!n.hasCondition()) {
            return;
        }

        try {
            conditionManager.verify(n.getCondition());
        } catch (ScriptException ex) {
            throw new StatusException("Invalid groovy condition: " + ex.getMessage());
        }
    }

    private void validateTemplate(EmailNotification en) {
        try {
            StringHelper.fromBase64(en.getHtmlTemplateInB64());
        } catch (IllegalArgumentException ignore) {
            throw new StatusException("Invalid b64 format");
        }
    }

    private void doSend(EmailNotification n) {

    }

    private void doSend(WebhookNotification n) {

    }
}
