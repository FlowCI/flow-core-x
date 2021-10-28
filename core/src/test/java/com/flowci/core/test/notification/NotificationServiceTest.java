package com.flowci.core.test.notification;

import com.flowci.core.common.domain.Variables;
import com.flowci.core.config.domain.SmtpConfig;
import com.flowci.core.config.service.ConfigService;
import com.flowci.core.job.domain.Job;
import com.flowci.core.notification.domain.EmailNotification;
import com.flowci.core.notification.domain.Notification;
import com.flowci.core.notification.service.NotificationService;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import com.flowci.util.StringHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.testng.Assert;

import java.io.IOException;
import java.util.Properties;

public class NotificationServiceTest extends SpringScenario {

    @Autowired
    private NotificationService notificationService;

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
        EmailNotification en = new EmailNotification();
        en.setName("default-email-notification");
        en.setSmtpConfig(config.getName());
        en.setTrigger(Notification.TriggerAction.OnJobStatusChange);
        en.setHtmlTemplateInB64(StringHelper.toBase64("Hello ${user}"));

        notificationService.save(en);

        Assert.assertNotNull(en.getId());
        Assert.assertNotNull(en.getCreatedBy());
        Assert.assertNotNull(en.getUpdatedBy());
    }

    @Test
    public void should_send_email_with_condition() throws IOException {
        EmailNotification en = new EmailNotification();
        en.setName("default-email-notification");
        en.setSmtpConfig(config.getName());
        en.setTrigger(Notification.TriggerAction.OnJobStatusChange);
        en.setHtmlTemplateInB64(StringHelper.toBase64(StringHelper.toString(load("email-template.html"))));

        Vars<String> context = new StringVars();
        context.put(Variables.Flow.Name, "ios-flow");
        context.put(Variables.Job.BuildNumber, "10");
        context.put(Variables.Job.Status, Job.Status.SUCCESS.name());
        context.put(Variables.Job.Trigger, Job.Trigger.PUSH.name());
        context.put(Variables.Job.StartAt, "2021-07-01 01:23:44.123");
        context.put(Variables.Job.FinishAt, "2021-07-01 02:23:45.456");

        notificationService.send(en, context);
    }

    private SmtpConfig getSmtpConfig() {
        SmtpConfig c = new SmtpConfig();
        c.setName("gmail-test");
        c.setServer("smtp.gmail.com");
        c.setPort(587);
        c.setSecure(SmtpConfig.SecureType.TLS);
        c.setAuth(SimpleAuthPair.of("tester", "tester"));
        return c;
    }

    private JavaMailSender getSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername("tester");
        mailSender.setPassword("tester");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.connectiontimeout", "2000");
        props.put("mail.smtp.timeout", "2000");
        props.put("mail.smtp.writetimeout", "2000");
        props.put("mail.smtp.auth", "true");
        props.put("mail.debug", "false");

        return mailSender;
    }
}
