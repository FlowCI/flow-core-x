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

package com.flow.platform.fsync;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import com.flow.platform.fsync.domain.FileSyncEvent;
import com.flow.platform.fsync.domain.FileSyncEventType;
import com.flow.platform.queue.DefaultQueueMessage;
import com.flow.platform.queue.InMemoryQueue;
import com.flow.platform.queue.PlatformQueue;
import com.flow.platform.util.Logger;
import com.flow.platform.util.StringUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author yang
 */
public class FileDispatcher implements Closeable {

    private final static Logger LOGGER = new Logger(FileChangeWorker.class);

    private final static int DEFAULT_CLIENT_QUEUE_PRIORITY = 1;

    private FileChangeWorker watcherWorker;

    private final Path watchPath;

    private final Executor executor;

    private final int clientQueueSize;

    private final Map<Path, FileSyncEvent> syncCache = new ConcurrentHashMap<>();

    private final Map<String, PlatformQueue<DefaultQueueMessage>> clientSyncEventQueue = new ConcurrentHashMap<>();

    private Consumer<FileSyncEvent> fileListener;

    private volatile boolean stop = false;

    public FileDispatcher(Path path, Executor executor, int clientQueueSize) throws IOException {
        this.watchPath = path;
        this.executor = executor;
        this.clientQueueSize = clientQueueSize;

        watcherWorker = new FileChangeWorker();
        path.register(watcherWorker.getWatcher(), ENTRY_CREATE, ENTRY_DELETE, OVERFLOW);
    }

    /**
     * Start watching
     */
    public void start() {
        stop = false;
        initFileSyncEvent();
        executor.execute(watcherWorker);
    }

    public void stop() {
        stop = true;
    }

    public Path getWatchPath() {
        return watchPath;
    }

    public void setFileListener(Consumer<FileSyncEvent> listener) {
        this.fileListener = listener;
    }

    public void addClient(String clientId) {
        removeClient(clientId);
        PlatformQueue<DefaultQueueMessage> queue = new InMemoryQueue<>(executor, clientQueueSize, clientId + "-queue");

        // init client file sync event into queue
        for (FileSyncEvent syncEvent : syncCache.values()) {
            queue.enqueue(new DefaultQueueMessage(syncEvent.toBytes(), DEFAULT_CLIENT_QUEUE_PRIORITY));
        }

        clientSyncEventQueue.put(clientId, queue);
    }

    public void removeClient(String clientId) {
        PlatformQueue<DefaultQueueMessage> exist = clientSyncEventQueue.remove(clientId);
        if (exist != null) {
            exist.cleanListener();
            exist.stop();
        }
    }

    public PlatformQueue<DefaultQueueMessage> getClientQueue(String clientId) {
        return clientSyncEventQueue.get(clientId);
    }

    public List<FileSyncEvent> files() {
        return ImmutableList.copyOf(syncCache.values());
    }

    @Override
    public void close() throws IOException {
        stop();
    }

    /**
     * Init file from watch path and watch event
     */
    private void initFileSyncEvent() {
        syncCache.clear();

        File[] files = watchPath.toFile().listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                Path path = file.toPath();
                syncCache.put(path, createFileSyncEvent(FileSyncEventType.CREATE, path));
            } catch (IOException e) {
                LOGGER.warn("Fail to init file sync event: " + e.getMessage());
            }
        }
    }

    private FileSyncEvent createFileSyncEvent(FileSyncEventType eventType, Path path) throws IOException {
        FileSyncEvent event = null;

        if (eventType == FileSyncEventType.CREATE) {
            long size = java.nio.file.Files.size(path);
            HashCode hash = Files.hash(path.toFile(), Hashing.md5());
            String checksum = hash.toString().toUpperCase();

            event = new FileSyncEvent(path.toString(), size, checksum, eventType);
            syncCache.put(path, event);
        }

        if (eventType == FileSyncEventType.DELETE) {
            syncCache.remove(path);
            event = new FileSyncEvent(path.toString(), 0L, StringUtil.EMPTY, eventType);
        }

        // distribute event to clients
        if (event != null) {
            for (Map.Entry<String, PlatformQueue<DefaultQueueMessage>> entry : clientSyncEventQueue.entrySet()) {
                entry.getValue().enqueue(new DefaultQueueMessage(event.toBytes(), DEFAULT_CLIENT_QUEUE_PRIORITY));
            }
        }

        return event;
    }

    /**
     * The worker to listen file system and process event
     */
    private class FileChangeWorker implements Runnable {

        private final WatchService watcher = FileSystems.getDefault().newWatchService();

        private final Map<Kind, FileSyncEventType> events = new HashMap<>();

        FileChangeWorker() throws IOException {
            events.put(ENTRY_CREATE, FileSyncEventType.CREATE);
            events.put(ENTRY_DELETE, FileSyncEventType.DELETE);
        }

        WatchService getWatcher() {
            return watcher;
        }

        @Override
        public void run() {
            for (;!stop;) {
                WatchKey key;
                try {
                    key = watcher.poll(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    LOGGER.warn(e.getMessage());
                    return;
                }

                if (key == null) {
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == OVERFLOW) {
                        continue;
                    }

                    // start new thread to get file size and checksum
                    Path fullPath = Paths.get(watchPath.toString(), event.context().toString());
                    executor.execute(new OnFileChangedWorker(fullPath, events.get(event.kind())));
                }

                key.reset();
            }
        }
    }

    /**
     * Worker to get file size and checksum, then sync to cache
     */
    private class OnFileChangedWorker implements Runnable {

        private final Path path;

        private final FileSyncEventType eventType;

        OnFileChangedWorker(Path path, FileSyncEventType eventType) {
            this.path = path;
            this.eventType = eventType;
        }

        @Override
        public void run() {
            try {
                FileSyncEvent syncEvent = createFileSyncEvent(eventType, path);

                if (fileListener != null && syncEvent != null) {
                    fileListener.accept(syncEvent);
                }
            } catch (IOException e) {
                LOGGER.warn("Cannot handle file changes: " + e.getMessage());
            }
        }
    }
}
