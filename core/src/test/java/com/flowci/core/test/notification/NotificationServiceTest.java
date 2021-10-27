package com.flowci.core.test.notification;

import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.config.domain.SmtpConfig;
import com.flowci.core.config.event.GetConfigEvent;
import com.flowci.core.notification.domain.EmailNotification;
import com.flowci.core.notification.domain.Notification;
import com.flowci.core.notification.service.NotificationService;
import com.flowci.core.test.SpringScenario;
import com.flowci.util.StringHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testng.Assert;

public class NotificationServiceTest extends SpringScenario {

    @Autowired
    private NotificationService notificationService;

    @MockBean
    private SpringEventManager eventManager;

    @Before
    public void login() {
        mockLogin();
    }

    @Test
    public void should_save_email_notification() {
        SmtpConfig smtp = new SmtpConfig();
        smtp.setName("default-smtp");

        GetConfigEvent mockEvent = new GetConfigEvent(this, smtp.getName());
        mockEvent.setFetched(smtp);
        Mockito.when(eventManager.publish(Mockito.any())).thenReturn(mockEvent);

        EmailNotification en = new EmailNotification();
        en.setName("default-email-notification");
        en.setSmtpConfig(smtp.getName());
        en.setTrigger(Notification.TriggerAction.OnJobStatusChange);
        en.setHtmlTemplateInB64(StringHelper.toBase64("Hello ${user}"));

        notificationService.save(en);

        Assert.assertNotNull(en.getId());
        Assert.assertNotNull(en.getCreatedBy());
        Assert.assertNotNull(en.getUpdatedBy());
    }

}
