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
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author yh@firim
 */
public class InstallConsumer {

    private ThreadPoolExecutor threadPoolExecutor;

    private BlockingQueue<Plugin> pluginBlockingQueue;

    private PluginService pluginService;

    public InstallConsumer(ThreadPoolExecutor threadPoolExecutor,
                           BlockingQueue<Plugin> pluginBlockingQueue,
                           PluginService pluginService) {
        this.threadPoolExecutor = threadPoolExecutor;
        this.pluginBlockingQueue = pluginBlockingQueue;
        this.pluginService = pluginService;
    }

    public void start() {
        threadPoolExecutor.execute(new QueueProcesser());
    }

    private class QueueProcesser implements Runnable {

        @Override
        public void run() {
            while (true) {
                System.out.println(String.format("InstallConsumer: Thread - %s start", Thread.currentThread().getId()));

                try {
                    Plugin plugin = pluginBlockingQueue.poll(1L, TimeUnit.SECONDS);

                    // start download
                    if (!Objects.isNull(plugin)) {
                        System.out.println(String
                            .format("InstallConsumer: Thread - %s start download", Thread.currentThread().getId()));

                        System.out.println(String
                            .format("InstallConsumer: Thread - %s finish download", Thread.currentThread().getId()));
                    }
                } catch (Throwable e) {
                    System.out.println(String
                        .format("InstallConsumer: Thread - %s, Exception - %s", Thread.currentThread().getId(),
                            e.getMessage()));
                }
            }
        }
    }
}
