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

package com.flow.platform.plugin.consumer;

import com.flow.platform.plugin.domain.Plugin;
import com.flow.platform.plugin.domain.PluginStatus;
import com.flow.platform.plugin.event.PluginListener;
import com.flow.platform.plugin.service.PluginService;
import com.flow.platform.util.Logger;
import com.google.common.base.Strings;
import java.nio.file.Path;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yh@firim
 */

@Component
public class PluginStatusChangedConsumer implements PluginListener<PluginStatus> {

    private final static Logger LOGGER = new Logger(PluginStatusChangedConsumer.class);

    @Autowired
    private PluginService pluginService;

    @PostConstruct
    private void init() {
        pluginService.registerListener(this);
    }

    @Override
    public void call(PluginStatus pluginStatus, String tag, Path path, String pluginName) {
        LOGGER.traceMarker("PluginStatusChangedConsumer", "Incoming Message PluginStatus:" + pluginStatus);

        Plugin plugin = pluginService.find(pluginName);

        switch (pluginStatus) {
            case PENDING:
            case DELETE:
                plugin.setStatus(pluginStatus.PENDING);
                break;
            case UPDATE:
            case INSTALLED:
                plugin.setStatus(pluginStatus.INSTALLED);
                break;
            case INSTALLING:
                plugin.setStatus(pluginStatus.INSTALLING);
                break;
        }

        if (!Strings.isNullOrEmpty(tag)) {
            plugin.setTag(tag);
        }
        pluginService.update(plugin);
    }
}
