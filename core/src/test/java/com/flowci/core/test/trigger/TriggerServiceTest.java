package com.flowci.core.test.trigger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.config.domain.SmtpConfig;
import com.flowci.core.config.service.ConfigService;
import com.flowci.core.git.domain.GitCommit;
import com.flowci.core.git.domain.GitUser;
import com.flowci.core.job.domain.Job;
import com.flowci.core.test.MockLoggedInScenario;
import com.flowci.core.trigger.domain.EmailTrigger;
import com.flowci.core.trigger.domain.Trigger;
import com.flowci.core.trigger.event.EmailTemplateParsedEvent;
import com.flowci.core.trigger.service.TriggerService;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import com.flowci.common.helper.StringHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class TriggerServiceTest extends MockLoggedInScenario {

    @Autowired
    private TriggerService triggerService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConfigService configService;

    private final SmtpConfig config = getSmtpConfig();

    private final JavaMailSender sender = getSender();

    @BeforeEach
    void mock() {
        Mockito.when(configService.get(config.getName())).thenReturn(config);
        Mockito.when(configService.getEmailSender(config.getName())).thenReturn(sender);
    }

    @Test
    void should_save_email_notification() {
        EmailTrigger en = new EmailTrigger();
        en.setName("default-email-notification");
        en.setSmtpConfig(config.getName());
        en.setEvent(Trigger.Event.OnJobFinished);
        en.setTemplate("default");

        triggerService.save(en);

        assertNotNull(en.getId());
        assertNotNull(en.getCreatedBy());
        assertNotNull(en.getUpdatedBy());
    }

    @Disabled
    @Test
    void should_send_email_with_condition() throws JsonProcessingException {
        EmailTrigger en = new EmailTrigger();
        en.setName("default-email-notification");
        en.setSmtpConfig(config.getName());
        en.setEvent(Trigger.Event.OnJobFinished);
        en.setTemplate("default");
        en.setFrom("tester@flow.ci");
        en.setTo("benqyang_2006@hotmail.com");
        en.setSubject("flow.ci ios-flow/#10 status");

        Vars<String> context = new StringVars();
        context.put(Variables.Flow.Name, "ios-flow");
        context.put(Variables.Job.BuildNumber, "10");
        context.put(Variables.Job.Status, Job.Status.SUCCESS.name());
        context.put(Variables.Job.Trigger, Job.Trigger.PUSH.name());
        context.put(Variables.Job.TriggerBy, "tester@flow.ci");
        context.put(Variables.Job.StartAt, "2021-07-01 01:23:44.123");
        context.put(Variables.Job.FinishAt, "2021-07-01 02:23:45.456");
        context.put(Variables.Job.DurationInSeconds, "100");

        var commits = new ArrayList<GitCommit>(2);
        commits.add(GitCommit.of("1", "first message", "2021-01-01", "https://xxx.xxx/xx/1", new GitUser().setEmail("test1@flow.ci")));
        commits.add(GitCommit.of("2", "second message", "2021-01-02", "https://xxx.xxx/xx/2", new GitUser().setEmail("test2@flow.ci")));

        context.put(Variables.Git.BRANCH, "master");
        context.put(Variables.Git.PUSH_MESSAGE, "hello test");
        context.put(Variables.Git.PUSH_COMMIT_LIST, StringHelper.toBase64(objectMapper.writeValueAsString(commits)));
        context.put(Variables.Git.PUSH_COMMIT_TOTAL, String.valueOf(2));

//        context.put(Variables.Git.PR_URL, "http://xxx/pr/id");
//        context.put(Variables.Git.PR_TITLE, "Pull request test");
//        context.put(Variables.Git.PR_NUMBER, "12");
//        context.put(Variables.Git.PR_MESSAGE, "hello pr message");
//        context.put(Variables.Git.PR_BASE_REPO_NAME, "flow-ci-base");
//        context.put(Variables.Git.PR_BASE_REPO_BRANCH, "master");
//        context.put(Variables.Git.PR_HEAD_REPO_NAME, "flow-ci-head");
//        context.put(Variables.Git.PR_HEAD_REPO_BRANCH, "developer");
//        context.put(Variables.Git.PR_TIME, "2021-08-01 02:23:45.456");

        addEventListener((ApplicationListener<EmailTemplateParsedEvent>) event -> {
            try {
                String template = event.getTemplate().replaceAll("\\s", "");
                String expected = StringHelper.toString(load("templates/email-template-expected-success.html")).replaceAll("\\s", "");
                assertEquals(expected, template);
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });

        triggerService.send(en, context);
    }

    private SmtpConfig getSmtpConfig() {
        SmtpConfig c = new SmtpConfig();
        c.setName("gmail-test");
        c.setServer("smtp.gmail.com");
        c.setPort(587);
        c.setSecure(SmtpConfig.SecureType.TLS);
        c.setAuth(SimpleAuthPair.of("tester@flow.ci", "xxx"));
        return c;
    }

    private JavaMailSender getSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername("tester@flow.ci");
        mailSender.setPassword("xxxx");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");

        return mailSender;
    }
}
