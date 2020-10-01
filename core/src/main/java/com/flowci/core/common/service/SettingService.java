package com.flowci.core.common.service;

import com.flowci.core.common.domain.Settings;

public interface SettingService {

    Settings get();

    void save(Settings settings);
}
