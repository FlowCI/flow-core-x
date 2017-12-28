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

package com.flow.platform.queue;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * @author yang
 */
public abstract class PlatformQueue<T> {

    private class EmptyQueueListener implements QueueListener<T> {

        @Override
        public void onQueueItem(T item) {

        }
    }

    protected final Executor executor;

    protected final int maxSize;

    protected final String name;

    protected QueueListener<T> listener = new EmptyQueueListener();

    public PlatformQueue(Executor executor, int maxSize, String name) {
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
        Objects.requireNonNull(listener);
        this.listener = listener;
    }

    public void cleanListener() {
        this.listener = null;
    }

    /**
     * Start queue consumer
     */
    public abstract void start();

    /**
     * Stop queue consumer
     */
    public abstract void stop();

    /**
     * Put queue item to queue
     */
    public abstract void enqueue(T item);

    /**
     * Get top item from queue
     */
    public abstract T dequeue();

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
     * Remove all items from queue
     */
    public abstract void clean();

    /**
     * Queue processor is running
     */
    public abstract boolean isRunning();
}
