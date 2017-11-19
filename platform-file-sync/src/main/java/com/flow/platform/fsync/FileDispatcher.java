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
import com.flow.platform.util.Logger;
import com.flow.platform.util.StringUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import java.io.Closeable;
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
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * @author yang
 */
public class FileDispatcher implements Closeable {

    private FileChangeWorker watcherWorker;

    private final Path watchPath;

    private final Executor executor;

    private final Map<Path, FileSyncEvent> fileSyncCache = new ConcurrentHashMap<>();

    private final Map<String, Queue<FileSyncEvent>> clientFileSyncQueue = new ConcurrentHashMap<>();

    public FileDispatcher(Path path, Executor executor) throws IOException {
        this.watchPath = path;
        this.executor = executor;

        watcherWorker = new FileChangeWorker();
        path.register(watcherWorker.getWatcher(), ENTRY_CREATE, ENTRY_DELETE, OVERFLOW);
        executor.execute(watcherWorker);
    }

    public void addClient(String clientId) {
        clientFileSyncQueue.put(clientId, new ConcurrentLinkedQueue<>(fileSyncCache.values()));
    }

    public void removeClient(String clientId) {
        clientFileSyncQueue.remove(clientId);
    }

    public List<FileSyncEvent> files() {
        return ImmutableList.copyOf(fileSyncCache.values());
    }

    @Override
    public void close() throws IOException {

    }

    /**
     * The worker to listen file system and process event
     */
    private class FileChangeWorker implements Runnable {

        private final WatchService watcher = FileSystems.getDefault().newWatchService();

        private final Map<Kind, FileSyncEventType> events = new HashMap<>();

        private FileChangeWorker() throws IOException {
            events.put(ENTRY_CREATE, FileSyncEventType.CREATE);
            events.put(ENTRY_DELETE, FileSyncEventType.DELETE);
        }

        WatchService getWatcher() {
            return watcher;
        }

        @Override
        public void run() {
            WatchKey key;
            try {
                while ((key = watcher.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == OVERFLOW) {
                            continue;
                        }

                        Path fullPath = Paths.get(watchPath.toString(), event.context().toString());
                        executor.execute(new OnFileEventHandler(fullPath, events.get(event.kind())));
                    }

                    key.reset();
                }
            } catch (InterruptedException e) {

            }
        }
    }

    /**
     * Calculate file checksum and put to file event queue
     */
    private class OnFileEventHandler implements Runnable {

        private final Logger LOGGER = new Logger(OnFileEventHandler.class);

        private final Path path;

        private final FileSyncEventType eventType;

        public OnFileEventHandler(Path path, FileSyncEventType eventType) {
            this.path = path;
            this.eventType = eventType;
        }

        @Override
        public void run() {
            try {
                if (eventType == FileSyncEventType.CREATE) {
                    long size = java.nio.file.Files.size(path);
                    HashCode hash = Files.hash(path.toFile(), Hashing.md5());
                    String checksum = hash.toString().toUpperCase();

                    LOGGER.trace("The file '%s' is '%s' with checksum '%s'", path, eventType, checksum);

                    FileSyncEvent eventForCreate = new FileSyncEvent(path.toString(), size, checksum, eventType);
                    fileSyncCache.put(path, eventForCreate);
                    return;
                }

                if (eventType == FileSyncEventType.DELETE) {
                    fileSyncCache.remove(path);

                    FileSyncEvent eventForDelete = new FileSyncEvent(path.toString(), 0L, StringUtil.EMPTY, eventType);
                }

            } catch (IOException e) {

            }
        }
    }
}
