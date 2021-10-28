package com.flowci.core.config.service;

import com.flowci.core.config.domain.Config;
import com.flowci.core.config.domain.SmtpOption;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

public interface ConfigService {

    Config get(String name);

    List<Config> list();

    List<Config> list(Config.Category category);

    /**
     * Create or update config
     */
    Config save(String name, SmtpOption option);

    Config save(String name, String text);

    Config delete(String name);

    JavaMailSender getEmailSender(String smtpConfig);
}
