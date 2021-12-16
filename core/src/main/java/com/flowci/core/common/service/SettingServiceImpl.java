package com.flowci.core.common.service;

import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.dao.SettingsDao;
import com.flowci.core.common.domain.Settings;
import com.flowci.core.common.domain.Variables;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Log4j2
@Service
public class SettingServiceImpl implements SettingService {

    @Autowired
    private Environment environment;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ServerProperties serverProperties;

    @Autowired
    private SettingsDao settingsDao;

    /**
     * Set default values of system setting
     * - FLOWCI_SERVER_URL or http://FLOWCI_SERVER_ADDRESS:FLOWCI_SERVER_PORT
     * - FLOWCI_WEB_URL
     */
    @EventListener(ContextRefreshedEvent.class)
    public void setDefaultValue() {
        Optional<Settings> optional = settingsDao.findByUniqueId(Settings.DefaultId);
        if (optional.isPresent()) {
            log.info("Default system settings is existed");
            return;
        }

        var s = new Settings();
        s.setServerUrl(environment.getProperty(
                Variables.App.ServerUrl,
                String.format("http://%s:%s", serverProperties.getAddress(), serverProperties.getPort())
        ));
        s.setWebUrl(environment.getProperty(Variables.App.WebUrl, "http://localhost:2015"));
        s.setSource(appProperties.getResourceDomain());
        settingsDao.save(s);
        log.info("Default system settings is created");
    }

    @Override
    public Settings get() {
        return settingsDao.findByUniqueId(Settings.DefaultId).get();
    }

    @Override
    public void save(Settings settings) {
        Settings o = get();
        o.setServerUrl(settings.getServerUrl());
        o.setWebUrl(settings.getWebUrl());
        settingsDao.save(o);
    }
}
