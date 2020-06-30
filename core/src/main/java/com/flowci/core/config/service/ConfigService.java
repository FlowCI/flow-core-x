package com.flowci.core.config.service;

import com.flowci.core.config.domain.AndroidSignOption;
import com.flowci.core.config.domain.Config;
import com.flowci.core.config.domain.SmtpOption;

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

    Config save(String name, AndroidSignOption option);

    Config delete(String name);
}
