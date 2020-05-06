package com.flowci.core.config.service;

import com.flowci.core.config.domain.Config;
import com.flowci.core.config.domain.SmtpConfig;

import java.util.List;

public interface ConfigService {

    Config get(String name);

    List<Config> list();

    List<Config> list(Config.Category category);

    Config create(SmtpConfig smtpConfig);

}
