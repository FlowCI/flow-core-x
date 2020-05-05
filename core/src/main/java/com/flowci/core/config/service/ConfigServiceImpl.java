package com.flowci.core.config.service;

import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.config.dao.ConfigDao;
import com.flowci.core.config.domain.Config;
import com.flowci.core.config.domain.SmtpConfig;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.event.GetSecretEvent;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Service
public class ConfigServiceImpl implements ConfigService {

    @Autowired
    private ConfigDao configDao;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private SpringEventManager eventManager;

    @Override
    public Config create(SmtpConfig smtp) {
        Objects.requireNonNull(smtp.getName(), "Config name is required");
        Objects.requireNonNull(smtp.getServer(), "Smtp server address is required");
        Objects.requireNonNull(smtp.getPort(), "Smtp port is required");

        if (smtp.hasSecret()) {
            GetSecretEvent event = eventManager.publish(new GetSecretEvent(this, smtp.getSecret()));
            Secret secret = event.getSecret();

            if (Objects.isNull(secret)) {
                throw new NotFoundException("The secret {0} not found", smtp.getSecret());
            }

            if (secret.getCategory() != Secret.Category.AUTH) {
                throw new ArgumentException("Invalid secret type");
            }

            smtp.setAuth((SimpleAuthPair) secret.toSimpleSecret());
        }

        return save(smtp);
    }

    @Override
    public Config get(String name) {
        Optional<Config> optional = configDao.findByName(name);
        if (!optional.isPresent()) {
            throw new NotFoundException("Configuration name {0} is not found", name);
        }
        return optional.get();
    }

    private <T extends Config> T save(T config) {
        try {
            Date now = Date.from(Instant.now());
            config.setUpdatedAt(now);
            config.setCreatedAt(now);
            config.setCreatedBy(sessionManager.getUserId());
            return configDao.insert(config);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Configuration name {0} is already defined", config.getName());
        }
    }
}
