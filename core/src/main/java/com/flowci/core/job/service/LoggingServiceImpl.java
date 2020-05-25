/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.job.service;

import com.flowci.core.common.helper.CacheHelper;
import com.flowci.core.common.manager.SocketPushManager;
import com.flowci.core.common.rabbit.QueueOperations;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.domain.ExecutedCmd;
import com.flowci.core.job.domain.Job;
import com.flowci.exception.NotFoundException;
import com.flowci.store.FileManager;
import com.flowci.store.Pathable;
import com.flowci.util.FileHelper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.google.common.collect.ImmutableList;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author yang
 */
@Log4j2
@Service
public class LoggingServiceImpl implements LoggingService {

    private static final Page<String> LogNotFound = new PageImpl<>(
            ImmutableList.of("Log not available"),
            PageRequest.of(0, 1),
            1L
    );

    private static final int FileBufferSize = 8000; // ~8k

    private static final Pathable LogPath = () -> "logs";

    private final Cache<String, BufferedReader> logReaderCache =
            CacheHelper.createLocalCache(10, 60, new ReaderCleanUp());

    @Autowired
    private String topicForLogs;

    @Autowired
    private SocketPushManager socketPushManager;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private QueueOperations loggingQueueManager;

    @Autowired
    private StepService stepService;

    @EventListener(ContextRefreshedEvent.class)
    public void onStart() {
        loggingQueueManager.startConsumer(true, message -> {
            // send JobProto.LogItem byte array to web directly
            socketPushManager.push(topicForLogs, message.getBody());
            return true;
        });
    }

    @Override
    public Page<String> read(ExecutedCmd cmd, Pageable pageable) {
        BufferedReader reader = getReader(cmd.getId());

        if (Objects.isNull(reader)) {
            return LogNotFound;
        }

        try (Stream<String> lines = reader.lines()) {
            int i = pageable.getPageNumber() * pageable.getPageSize();

            List<String> logs = lines.skip(i)
                    .limit(pageable.getPageSize())
                    .collect(Collectors.toList());

            return new PageImpl<>(logs, pageable, cmd.getLogSize());
        } finally {
            try {
                reader.reset();
            } catch (IOException e) {
                // reset will be failed if all lines been read
                logReaderCache.invalidate(cmd.getId());
            }
        }
    }

    @Override
    public String save(String fileName, InputStream stream) throws IOException {
        String cmdId = FileHelper.getName(fileName);
        Pathable[] logDir = getLogDir(cmdId);
        return fileManager.save(fileName, stream, logDir);
    }

    @Override
    public Resource get(String cmdId) {
        try {
            String fileName = getLogFile(cmdId);
            InputStream stream = fileManager.read(fileName, getLogDir(cmdId));
            return new InputStreamResource(stream);
        } catch (IOException e) {
            throw new NotFoundException("Log not available");
        }
    }

    private BufferedReader getReader(String cmdId) {
        return logReaderCache.get(cmdId, key -> {
            try {
                String fileName = getLogFile(cmdId);
                InputStream stream = fileManager.read(fileName, getLogDir(cmdId));
                InputStreamReader streamReader = new InputStreamReader(stream);
                BufferedReader reader = new BufferedReader(streamReader, FileBufferSize);
                reader.mark(1);
                return reader;
            } catch (IOException e) {
                return null;
            }
        });
    }

    private Pathable[] getLogDir(String cmdId) {
        ExecutedCmd cmd = stepService.get(cmdId);

        return new Pathable[]{
                Flow.path(cmd.getFlowId()),
                Job.path(cmd.getBuildNumber()),
                LogPath
        };
    }

    private String getLogFile(String cmdId) {
        return cmdId + ".log";
    }

    private static class ReaderCleanUp implements RemovalListener<String, BufferedReader> {

        @Override
        public void onRemoval(String key, BufferedReader reader, RemovalCause cause) {
            if (Objects.isNull(reader)) {
                return;
            }

            try {
                reader.close();
            } catch (IOException e) {
                log.debug(e);
            }
        }
    }
}
