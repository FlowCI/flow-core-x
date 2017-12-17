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

package com.flow.platform.api.consumer;

import com.flow.platform.api.domain.sync.SyncType;
import com.flow.platform.api.service.SyncService;
import com.flow.platform.plugin.domain.PluginStatus;
import com.flow.platform.plugin.event.PluginStatusChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */

@Component
public class PluginEventConsumer implements ApplicationListener<PluginStatusChangeEvent> {

    @Autowired
    private SyncService syncService;

    @Override
    public void onApplicationEvent(PluginStatusChangeEvent event) {
        final String name = event.getPluginName();
        final String tag = event.getTag();
        final PluginStatus status = event.getPluginStatus();

        SyncType syncType = null;

        switch (status) {
            case INSTALLED:
                syncType = SyncType.CREATE;
                break;

            case DELETE:
                syncType = SyncType.DELETE;
                break;

            default:
                // do not handle other sync type
                break;
        }

        if (syncType == null) {
            return;
        }

        syncService.put(name, tag, syncType);
    }
}
