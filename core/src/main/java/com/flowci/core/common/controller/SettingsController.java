package com.flowci.core.common.controller;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.common.domain.Settings;
import com.flowci.core.common.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/settings")
public class SettingsController {

    @Autowired
    private SettingService settingService;

    @GetMapping
    @Action(Settings.Action.GET)
    public Settings get() {
        return settingService.get();
    }

    @PostMapping
    @Action(Settings.Action.UPDATE)
    public void save(@RequestBody Settings settings) {
        settingService.save(settings);
    }
}
