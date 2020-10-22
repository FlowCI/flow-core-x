package com.flowci.core.common.service;

import com.flowci.core.common.dao.SettingsDao;
import com.flowci.core.common.domain.Settings;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.manager.SpringTaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Service
public class SettingServiceImpl implements SettingService {

    @Autowired
    private Environment environment;

    @Autowired
    private ServerProperties serverProperties;

    @Autowired
    private SettingsDao settingsDao;

    @Autowired
    private SpringTaskManager taskManager;

    /**
     * Set default server url
     * value from FLOWCI_SERVER_URL or http://FLOWCI_SERVER_ADDRESS:FLOWCI_SERVER_PORT
     */
    @PostConstruct
    public void setDefaultValue() {
        taskManager.run("init-default-settings", () -> {
            Optional<Settings> optional = settingsDao.findById(Settings.DefaultId);

            String address = serverProperties.getAddress().toString().replace("/", "");
            String serverUrl = environment.getProperty(
                    Variables.App.ServerUrl,
                    String.format("http://%s:%s", address, serverProperties.getPort())
            );

            Settings s = new Settings();

            if (optional.isPresent()) {
                s = optional.get();
            }

            s.setServerUrl(serverUrl);
            settingsDao.save(s);
        });
    }

    @Override
    public Settings get() {
        return settingsDao.findById(Settings.DefaultId).get();
    }

    @Override
    public void save(Settings settings) {
        Settings o = get();
        o.setServerUrl(settings.getServerUrl());

        settingsDao.save(o);
    }
}
