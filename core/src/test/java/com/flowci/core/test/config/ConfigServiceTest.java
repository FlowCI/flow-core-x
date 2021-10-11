package com.flowci.core.test.config;

import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.config.domain.SmtpConfig;
import com.flowci.core.config.domain.SmtpOption;
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

import static com.flowci.core.config.domain.Config.Category.SMTP;
import static com.flowci.core.config.domain.Config.Category.TEXT;

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
        mockEvent.setFetched(mockSecret);
        Mockito.when(eventManager.publish(Mockito.any())).thenReturn(mockEvent);

        // when:
        SmtpOption option = new SmtpOption();
        option.setServer("smtp.google.com");
        option.setPort(25);
        option.setSecret(mockSecret.getName());
        configService.save("smtp-config", option);

        // then:
        SmtpConfig config = (SmtpConfig) configService.get("smtp-config");
        should_has_db_info(config);

        Assert.assertNotNull(config.getId());
        Assert.assertEquals(mockSecret.getUsername(), config.getAuth().getUsername());
        Assert.assertEquals(mockSecret.getPassword(), config.getAuth().getPassword());

        // then: test list
        Assert.assertEquals(1, configService.list().size());
        Assert.assertEquals(config, configService.list().get(0));
        Assert.assertEquals(1, configService.list(SMTP).size());
        Assert.assertEquals(0, configService.list(TEXT).size());
    }
}
