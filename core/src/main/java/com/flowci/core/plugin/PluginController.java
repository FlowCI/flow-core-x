/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.plugin;

import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.service.PluginService;
import com.flowci.util.StringHelper;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Collection;

/**
 * @author yang
 */
@RestController
@RequestMapping("/plugins")
public class PluginController {

    @Autowired
    private PluginService pluginService;

    @GetMapping()
    public Collection<Plugin> installed(@RequestParam(required = false) String tags) {
        // tags format is tags=a,b,c,d
        if (StringHelper.hasValue(tags)) {
            String[] split = tags.split(",");
            if (split.length == 0) {
                return pluginService.list();
            }

            return pluginService.list(Sets.newHashSet(split));
        }

        return pluginService.list();
    }

    @GetMapping(value = "/{name}/readme", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getReadMeContent(@PathVariable String name) {
        Plugin plugin = pluginService.get(name);
        byte[] raw = pluginService.getReadMe(plugin);
        return Base64.getEncoder().encodeToString(raw);
    }

    @GetMapping(value = "/{name}/icon", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getPluginIcon(@PathVariable String name) {
        Plugin plugin = pluginService.get(name);

        if (StringHelper.isHttpLink(plugin.getMeta().getIcon())) {
            return StringHelper.EMPTY;
        }

        byte[] raw = pluginService.getIcon(plugin);
        return Base64.getEncoder().encodeToString(raw);
    }

    @PostMapping("/reload")
    public void reload() {
        pluginService.reload();
    }
}
