/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.api.controller;

import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.plugin.domain.Plugin;
import com.flow.platform.plugin.service.PluginService;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yh@firim
 */
@RestController
@RequestMapping("/plugins")
public class PluginController {

    @Autowired
    private PluginService pluginService;

    @GetMapping
    public List<Plugin> index() {
        return pluginService.list();
    }

    @PostMapping("/install")
    public void install(@RequestParam String name) {
        if(Objects.isNull(name)){
            throw new IllegalParameterException("plugin name is null");
        }
        pluginService.install(name);
    }

    @DeleteMapping("/uninstall")
    public void uninstall(@RequestParam String name) {
        if(Objects.isNull(name)){
            throw new IllegalParameterException("plugin name is null");
        }
        pluginService.uninstall(name);
    }
}
