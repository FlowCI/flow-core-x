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
import com.flow.platform.plugin.event.PluginStatusChangeEvent;
import com.flow.platform.plugin.service.PluginService;
import com.google.common.base.Strings;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author yh@firim
 */
@Component
public class PluginStatusChangedConsumer implements ApplicationListener<PluginStatusChangeEvent> {

    @Autowired
    private PluginService pluginService;

    @Override
    public void onApplicationEvent(PluginStatusChangeEvent event) {

        doSendEventToApi(event);

        doUpdatePlugin(event);
    }

    private void doSendEventToApi(PluginStatusChangeEvent event) {

        // only installed delete update send to Api
        if (Arrays.asList(PluginStatus.DELETE, PluginStatus.INSTALLED, PluginStatus.UPDATE)
            .contains(event.getPluginStatus())) {
            String tag = event.getTag();
            String pluginName = event.getPluginName();

            //TODO: callback
        }
    }

    private void doUpdatePlugin(PluginStatusChangeEvent event) {
        Plugin plugin = pluginService.find(event.getPluginName());

        switch (event.getPluginStatus()) {
            case PENDING:
            case IN_QUEUE:
                plugin.setStatus(event.getPluginStatus());
                break;
            case INSTALLING:
            case INSTALLED:
                plugin.setStatus(event.getPluginStatus());
                break;
            case UPDATE:
                plugin.setStatus(PluginStatus.INSTALLED);
                break;
            case DELETE:
                plugin.setStatus(PluginStatus.PENDING);
                break;
        }

        if (!Strings.isNullOrEmpty(event.getTag())) {
            plugin.setTag(event.getTag());
        }

        pluginService.update(plugin);
    }
}
