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

import com.flow.platform.util.Logger;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author yang
 */
public class InMemoryQueue<T extends Comparable> extends PlatformQueue<T> {

    private final Logger LOGGER = new Logger(InMemoryQueue.class);

    private final PriorityBlockingQueue<T> queue;

    private final Object lock = new Object();

    private volatile boolean stop = false;

    private volatile boolean pause = false;

    public InMemoryQueue(Executor executor, int maxSize, String name) {
        super(executor, maxSize, name);
        this.queue = new PriorityBlockingQueue<>(maxSize);
    }

    public InMemoryQueue(Executor executor, int maxSize, String name, Comparator<T> comparator) {
        super(executor, maxSize, name);
        this.queue = new PriorityBlockingQueue<>(maxSize, comparator);
    }

    @Override
    public void start() {
        stop = false;
        executor.execute(new QueueProcessor());
    }

    @Override
    public void stop() {
        cleanListener();
        stop = true;
    }

    @Override
    public void enqueue(T item) {
        queue.offer(item);
    }

    @Override
    public T dequeue() {
        try {
            return queue.poll(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public void pause() {
        if (pause) {
            return;
        }

        pause = true;
    }

    @Override
    public void resume() {
        if (!pause) {
            return;
        }

        synchronized (lock) {
            lock.notifyAll();
        }

        pause = false;
    }

    @Override
    public void clean() {
        queue.clear();
    }

    @Override
    public boolean isRunning() {
        return !pause && !stop;
    }

    private class QueueProcessor implements Runnable {

        @Override
        public void run() {
            while (!stop) {

                synchronized (lock) {
                    if (pause) {
                        try {
                            lock.wait();
                        } catch (InterruptedException ignore) {
                        }
                    }
                }

                try {
                    T item = queue.poll(1000, TimeUnit.SECONDS);

                    if (Objects.isNull(item)) {
                        continue;
                    }

                    for (QueueListener<T> listener : listeners) {
                        listener.onQueueItem(item);
                    }

                } catch (InterruptedException ignore) {
                    LOGGER.warn("InterruptedException occurred while queue processing: ", ignore.getMessage());
                }
            }
        }
    }
}
