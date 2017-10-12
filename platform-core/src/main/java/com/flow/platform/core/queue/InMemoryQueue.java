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

package com.flow.platform.core.queue;

import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.util.Logger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
public class InMemoryQueue<T> extends PlatformQueue<T> {

    private final Logger LOGGER = new Logger(InMemoryQueue.class);

    private final BlockingQueue<T> queue;

    private volatile boolean shouldStop = false;

    public InMemoryQueue(ThreadPoolTaskExecutor executor, int maxSize) {
        super(executor, maxSize);
        this.queue = new LinkedBlockingQueue<>(maxSize);
    }

    @Override
    public void start() {
        executor.execute(() -> {
            while (!shouldStop) {
                try {
                    T item = queue.poll(1L, TimeUnit.SECONDS);
                    for (QueueListener<T> listener : listeners) {
                        listener.onQueueItem(item);
                    }
                } catch (InterruptedException ignore) {
                    LOGGER.warn("InterruptedException occurred while queue processing: ", ignore.getMessage());
                }
            }
        });
    }

    @Override
    public void stop() {
        shouldStop = true;
    }

    @Override
    public void enqueue(T item) {
        try {
            queue.put(item);
        } catch (InterruptedException e) {
            LOGGER.warn("InterruptedException occurred while enqueue: ", e.getMessage());
            throw new IllegalStatusException("Unable to queue since thread interrupted");
        }
    }
}
