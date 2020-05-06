package com.flowci.core.test.config;

import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.config.domain.SmtpConfig;
import com.flowci.core.config.service.ConfigService;
import com.flowci.core.secret.domain.AuthSecret;
import com.flowci.core.secret.event.GetSecretEvent;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.SimpleAuthPair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.flowci.core.config.domain.Config.Category.FILE;
import static com.flowci.core.config.domain.Config.Category.SMTP;

public class ConfigServiceTest extends SpringScenario {

    @MockBean
    private SpringEventManager eventManager;

    @Autowired
    private ConfigService configService;

    @Before
    public void init() {
        mockLogin();
    }

    @Test
    public void should_create_smtp_config_with_secret() {
        // init:
        AuthSecret mockSecret = new AuthSecret();
        mockSecret.setName("smtp-secret");
        mockSecret.setPair(SimpleAuthPair.of("test@gmail.com", "12345"));

        GetSecretEvent mockEvent = new GetSecretEvent(this, mockSecret.getName());
        mockEvent.setSecret(mockSecret);
        Mockito.when(eventManager.publish(Mockito.any())).thenReturn(mockEvent);

        // when:
        SmtpConfig config = new SmtpConfig();
        config.setName("smtp-config");
        config.setServer("smtp.google.com");
        config.setPort(25);
        config.setSecret(mockSecret.getName());
        configService.create(config);

        // then:
        config = (SmtpConfig) configService.get(config.getName());
        Assert.assertNotNull(config.getId());
        Assert.assertEquals(mockSecret.getUsername(), config.getAuth().getUsername());
        Assert.assertEquals(mockSecret.getPassword(), config.getAuth().getPassword());

        // then: test list
        Assert.assertEquals(1, configService.list().size());
        Assert.assertEquals(config, configService.list().get(0));
        Assert.assertEquals(1, configService.list(SMTP).size());
        Assert.assertEquals(0, configService.list(FILE).size());
    }
}
