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

package com.flow.platform.core.consumer;

import com.flow.platform.core.context.ContextEvent;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
public abstract class QueueConsumer<T> implements ContextEvent {

    public abstract ThreadPoolTaskExecutor getTaskExecutor();

    private volatile boolean shouldStop = false;

    public abstract BlockingQueue<T> getQueue();

    public abstract void onQueueItem(T item);

    @Override
    public void start() {
        getTaskExecutor().execute(() -> {
            while (!shouldStop) {
                try {
                    T item = getQueue().poll(1L, TimeUnit.SECONDS);
                    onQueueItem(item);
                } catch (InterruptedException ignore) {

                }
            }
        });
    }

    @Override
    public void stop() {
        shouldStop = true;
    }
}
