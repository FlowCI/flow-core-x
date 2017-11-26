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
import com.flow.platform.plugin.service.PluginService;
import com.flow.platform.queue.PlatformQueue;
import com.flow.platform.queue.QueueListener;
import com.flow.platform.util.Logger;

/**
 * @author yh@firim
 */
public class InstallConsumer implements QueueListener<Plugin> {

    private final static Logger LOGGER = new Logger(InstallConsumer.class);
    private PlatformQueue<Plugin> installPluginsQueue;

    private PluginService pluginService;

    public InstallConsumer(PlatformQueue<Plugin> installPluginsQueue, PluginService pluginService) {
        this.installPluginsQueue = installPluginsQueue;
        this.pluginService = pluginService;
        installPluginsQueue.register(this);
    }

    @Override
    public void onQueueItem(Plugin item) {

        LOGGER.traceMarker("InstallConsumer",
            String.format("Thread: %s, Start Install Plugin", Thread.currentThread().getId()));

        // out stack install
        pluginService.execInstallOrUpdate(item);

        LOGGER.traceMarker("InstallConsumer",
            String.format("Thread: %s, Finish Install Plugin", Thread.currentThread().getId()));
    }
}
