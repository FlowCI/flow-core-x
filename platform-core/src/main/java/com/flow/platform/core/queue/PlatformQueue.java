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

import com.flow.platform.core.context.ContextEvent;
import java.util.LinkedList;
import java.util.List;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
public abstract class PlatformQueue<T> implements ContextEvent {

    protected final List<QueueListener<T>> listeners = new LinkedList<>();

    protected final ThreadPoolTaskExecutor executor;

    protected final int maxSize;

    protected final String name;

    public PlatformQueue(ThreadPoolTaskExecutor executor, int maxSize, String name) {
        this.executor = executor;
        this.maxSize = maxSize;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Register queue item listener
     */
    public void register(QueueListener<T> listener) {
        this.listeners.add(listener);
    }

    public void cleanListener() {
        this.listeners.clear();
    }

    /**
     * Put queue item to queue
     */
    public abstract void enqueue(T item);

    /**
     * Get current queue size
     */
    public abstract int size();

    /**
     * Hold queue which not process queue item
     */
    public abstract void pause();

    /**
     * Resume queue
     */
    public abstract void resume();

    /**
     * Queue processor is running
     */
    public abstract boolean isRunning();
}
