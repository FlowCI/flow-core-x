package com.flowci.core.test.config;

import com.flowci.core.config.domain.Config;
import com.flowci.core.config.domain.ConfigParser;
import com.flowci.core.config.domain.SmtpConfig;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;

public class ConfigParserTest {

    @Test
    public void should_parse_smtp_config_yml() {
        InputStream is = ConfigParserTest.class.getClassLoader().getResourceAsStream("smtp-config.yml");
        Config config = ConfigParser.parse(is);
        Assert.assertNotNull(config);
        Assert.assertEquals(Config.Category.SMTP, config.getCategory());

        SmtpConfig smtp = (SmtpConfig) config;
        Assert.assertEquals("my-demo", smtp.getName());
        Assert.assertEquals("smtp.sendgrid.net", smtp.getServer());
        Assert.assertEquals(587, smtp.getPort().intValue());
        Assert.assertEquals("apikey", smtp.getAuth().getUsername());
        Assert.assertEquals("123", smtp.getAuth().getPassword());
        Assert.assertEquals(SmtpConfig.SecureType.TLS, smtp.getSecure());
    }
}
