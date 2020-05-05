package com.flowci.core.config.service;

import com.flowci.core.config.domain.Config;
import com.flowci.core.config.domain.SmtpConfig;

public interface ConfigService {

    Config create(SmtpConfig smtpConfig);

    Config get(String name);
}
