package com.flowci.core.test.notification;

import com.flowci.core.common.domain.Variables;
import com.flowci.core.config.domain.SmtpConfig;
import com.flowci.core.config.service.ConfigService;
import com.flowci.core.job.domain.Job;
import com.flowci.core.trigger.domain.EmailTrigger;
import com.flowci.core.trigger.domain.Trigger;
import com.flowci.core.trigger.event.EmailTemplateParsedEvent;
import com.flowci.core.trigger.service.TriggerService;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import com.flowci.util.StringHelper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.testng.Assert;

import java.io.IOException;
import java.util.Properties;

public class TriggerServiceTest extends SpringScenario {

    @Autowired
    private TriggerService triggerService;

    @MockBean
    private ConfigService configService;

    private final SmtpConfig config = getSmtpConfig();

    private final JavaMailSender sender = getSender();

    @Before
    public void login() {
        mockLogin();
    }

    @Before
    public void mock() {
        Mockito.when(configService.get(config.getName())).thenReturn(config);
        Mockito.when(configService.getEmailSender(config.getName())).thenReturn(sender);
    }

    @Test
    public void should_save_email_notification() {
        EmailTrigger en = new EmailTrigger();
        en.setName("default-email-notification");
        en.setSmtpConfig(config.getName());
        en.setAction(Trigger.Action.OnJobFinished);
        en.setTemplate("default");

        triggerService.save(en);

        Assert.assertNotNull(en.getId());
        Assert.assertNotNull(en.getCreatedBy());
        Assert.assertNotNull(en.getUpdatedBy());
    }

    @Ignore
    @Test
    public void should_send_email_with_condition() {
        EmailTrigger en = new EmailTrigger();
        en.setName("default-email-notification");
        en.setSmtpConfig(config.getName());
        en.setAction(Trigger.Action.OnJobFinished);
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

        context.put(Variables.Git.GIT_BRANCH, "master");
        context.put(Variables.Git.GIT_COMMIT_URL, "http://xxx/commit/id");
        context.put(Variables.Git.GIT_COMMIT_ID, "112233");
        context.put(Variables.Git.GIT_COMMIT_MESSAGE, "hello test");

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
                Assert.assertEquals(expected, template);
            } catch (IOException e) {
                Assert.fail();
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
