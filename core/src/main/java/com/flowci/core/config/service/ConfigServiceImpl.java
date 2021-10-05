package com.flowci.core.config.service;

import com.flowci.core.common.domain.Mongoable;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.config.dao.ConfigDao;
import com.flowci.core.config.domain.Config;
import com.flowci.core.config.domain.SmtpConfig;
import com.flowci.core.config.domain.SmtpOption;
import com.flowci.core.config.domain.TextConfig;
import com.flowci.core.config.event.GetConfigEvent;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.event.GetSecretEvent;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class ConfigServiceImpl implements ConfigService {

    @Autowired
    private ConfigDao configDao;

    @Autowired
    private SpringEventManager eventManager;

    @EventListener
    public void onGetConfigEvent(GetConfigEvent event) {
        try {
            Config config = get(event.getName());
            event.setFetched(config);
        } catch (NotFoundException e) {
            event.setError(e);
        }
    }

    @Override
    public Config get(String name) {
        Optional<Config> optional = configDao.findByName(name);
        if (!optional.isPresent()) {
            throw new NotFoundException("Configuration name {0} is not found", name);
        }
        return optional.get();
    }

    @Override
    public List<Config> list() {
        return configDao.findAll(Mongoable.SortByCreatedAtASC);
    }

    @Override
    public List<Config> list(Config.Category category) {
        return configDao.findAllByCategoryOrderByCreatedAtAsc(category);
    }

    @Override
    public Config save(String name, SmtpOption option) {
        try {
            SmtpConfig config = load(name, SmtpConfig.class);
            config.setServer(option.getServer());
            config.setPort(option.getPort());
            config.setSecure(option.getSecure());
            config.setAuth(option.getAuth());

            if (option.hasSecret()) {
                config.setAuth(getAuthPairFromSecret(option));
            }

            return save(config);
        } catch (ReflectiveOperationException e) {
            throw new StatusException(e.getMessage());
        }
    }

    @Override
    public Config save(String name, String text) {
        try {
            TextConfig config = load(name, TextConfig.class);
            config.setText(text);
            return save(config);
        } catch (ReflectiveOperationException e) {
            throw new StatusException(e.getMessage());
        }
    }

    private <T extends Config> T load(String name, Class<T> tClass) throws ReflectiveOperationException {
        Optional<Config> optional = configDao.findByName(name);

        T config;
        if (optional.isPresent()) {
            config = (T) optional.get();
        } else {
            config = tClass.newInstance();
            config.setName(name);
        }

        return config;
    }

    private <T extends Config> T save(T config) {
        try {
            return configDao.save(config);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Config name {0} is already defined", config.getName());
        }
    }

    private SimpleAuthPair getAuthPairFromSecret(SmtpOption option) {
        GetSecretEvent event = eventManager.publish(new GetSecretEvent(this, option.getSecret()));
        if (event.hasError()) {
            throw event.getError();
        }

        Secret secret = event.getFetched();
        if (secret.getCategory() != Secret.Category.AUTH) {
            throw new ArgumentException("Invalid secret type");
        }

        return (SimpleAuthPair) secret.toSimpleSecret();
    }

    @Override
    public Config delete(String name) {
        Config config = get(name);
        configDao.deleteById(config.getId());
        return config;
    }
}
