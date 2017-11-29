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

package com.flow.platform.plugin.context;

import com.flow.platform.plugin.dao.PluginDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * @author yh@firim
 */
@Component
public class PluginContextCloseHandler implements ApplicationListener<ContextClosedEvent> {

    @Autowired
    private PluginDao pluginStoreService;

    @Autowired
    private ThreadPoolTaskExecutor pluginPoolExecutor;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {

        // shutdown executor
        pluginPoolExecutor.setWaitForTasksToCompleteOnShutdown(true);
        pluginPoolExecutor.setAwaitTerminationSeconds(10);
        pluginPoolExecutor.shutdown();

        // dump cache to file
        pluginStoreService.dumpCacheToFile();

    }
}
