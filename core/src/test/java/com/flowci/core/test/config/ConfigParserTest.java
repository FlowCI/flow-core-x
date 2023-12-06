package com.flowci.core.test.config;

import com.flowci.core.config.domain.Config;
import com.flowci.core.config.domain.ConfigParser;
import com.flowci.core.config.domain.SmtpConfig;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ConfigParserTest {

    @Test
    void should_parse_smtp_config_yml() {
        InputStream is = ConfigParserTest.class.getClassLoader().getResourceAsStream("smtp-config.yml");
        Config config = ConfigParser.parse(is);
        assertNotNull(config);
        assertEquals(Config.Category.SMTP, config.getCategory());

        SmtpConfig smtp = (SmtpConfig) config;
        assertEquals("my-demo", smtp.getName());
        assertEquals("smtp.sendgrid.net", smtp.getServer());
        assertEquals(587, smtp.getPort().intValue());
        assertEquals("apikey", smtp.getAuth().getUsername());
        assertEquals("123", smtp.getAuth().getPassword());
        assertEquals(SmtpConfig.SecureType.TLS, smtp.getSecure());
    }
}
