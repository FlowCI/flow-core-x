package com.flowci.core.common.service;

import com.flowci.core.common.dao.SettingsDao;
import com.flowci.core.common.domain.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Service
public class SettingServiceImpl implements SettingService {

    @Autowired
    private ServerProperties serverProperties;

    @Autowired
    private SettingsDao settingsDao;

    @PostConstruct
    public void setDefaultValue() {
        Optional<Settings> optional = settingsDao.findById(Settings.DefaultId);
        if (!optional.isPresent()) {
            Settings s = new Settings();
            s.setServerUrl(String.format("http://%s:%s", serverProperties.getAddress(), serverProperties.getPort()));
            settingsDao.save(s);
        }
    }

    @Override
    public Settings get() {
        return settingsDao.findById(Settings.DefaultId).get();
    }

    @Override
    public void save(Settings settings) {
        settingsDao.save(settings);
    }
}
