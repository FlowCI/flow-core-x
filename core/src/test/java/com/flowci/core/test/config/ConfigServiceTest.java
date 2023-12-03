package com.flowci.core.test.config;

import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.config.domain.SmtpConfig;
import com.flowci.core.config.domain.SmtpOption;
import com.flowci.core.config.service.ConfigService;
import com.flowci.core.secret.domain.AuthSecret;
import com.flowci.core.secret.event.GetSecretEvent;
import com.flowci.core.test.MockLoggedInScenario;
import com.flowci.domain.SimpleAuthPair;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.flowci.core.config.domain.Config.Category.SMTP;
import static com.flowci.core.config.domain.Config.Category.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ConfigServiceTest extends MockLoggedInScenario {

    @MockBean
    private SpringEventManager eventManager;

    @Autowired
    private ConfigService configService;

    @Test
    void should_create_smtp_config_with_secret() {
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
        shouldHasCreatedAtAndCreatedBy(config);

        assertNotNull(config.getId());
        assertEquals(mockSecret.getUsername(), config.getAuth().getUsername());
        assertEquals(mockSecret.getPassword(), config.getAuth().getPassword());

        // then: test list
        assertEquals(1, configService.list().size());
        assertEquals(config, configService.list().get(0));
        assertEquals(1, configService.list(SMTP).size());
        assertEquals(0, configService.list(TEXT).size());
    }
}
