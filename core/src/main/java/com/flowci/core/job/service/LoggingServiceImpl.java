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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.agent.event.OnShellLogEvent;
import com.flowci.core.agent.event.OnTTYLogEvent;
import com.flowci.core.common.helper.CacheHelper;
import com.flowci.core.common.manager.SocketPushManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Step;
import com.flowci.core.job.domain.StepLogItem;
import com.flowci.core.job.event.CacheShellLogEvent;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.exception.NotFoundException;
import com.flowci.store.FileManager;
import com.flowci.store.Pathable;
import com.flowci.util.FileHelper;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.ImmutableList;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    // cache current job log
    private final Cache<String, Map<String, Queue<byte[]>>> logCache = CacheHelper.createLocalCache(50, 3600);

    @Autowired
    private String topicForTtyLogs;

    @Autowired
    private String topicForLogs;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SocketPushManager socketPushManager;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private StepService stepService;

    @EventListener
    public void cacheShellLog(CacheShellLogEvent event) {
        Map<String, Queue<byte[]>> cache = logCache.getIfPresent(event.getJobId());
        if (cache != null) {
            cache.get(event.getStepId()).add(event.getBody());
        }
    }

    @EventListener
    public void sendTtyLogToClient(OnTTYLogEvent event) {
        String ttyId = event.getTtyId();
        socketPushManager.push(topicForTtyLogs + "/" + ttyId, event.getBody().getBytes());
    }

    @EventListener
    public void sendShellLogToClient(OnShellLogEvent event) {
        String jobId = event.getJobId();
        String stepId = event.getStepId();
        String b64Log = event.getB64Log();

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(new StepLogItem(stepId, b64Log));

            eventManager.publish(new CacheShellLogEvent(this, jobId, stepId, bytes));
            socketPushManager.push(topicForLogs + "/" + jobId, bytes);
        } catch (JsonProcessingException e) {
            log.warn(e);
        }
    }

    @EventListener
    public void handleLogCacheForJob(JobStatusChangeEvent event) {
        Job job = event.getJob();

        if (job.getStatus() == Job.Status.CREATED) {
            List<Step> steps = stepService.list(job);
            Map<String, Queue<byte[]>> cache = new HashMap<>(steps.size());
            for (Step step : steps) {
                cache.put(step.getId(), new ConcurrentLinkedQueue<>());
            }
            logCache.put(job.getId(), cache);
            return;
        }

        if (job.isDone()) {
            logCache.invalidate(job.getId());
        }
    }

    @Override
    public String save(String fileName, InputStream stream) throws IOException {
        String cmdId = FileHelper.getName(fileName);
        Pathable[] logDir = getLogDir(cmdId);
        return fileManager.save(fileName, stream, logDir);
    }

    @Override
    public Resource get(String stepId) {
        try {
            String fileName = getLogFile(stepId);
            InputStream stream = fileManager.read(fileName, getLogDir(stepId));
            return new InputStreamResource(stream);
        } catch (IOException e) {
            throw new NotFoundException("Log not available");
        }
    }

    @Override
    public Collection<byte[]> read(String stepId) {
        Step step = stepService.get(stepId);
        Map<String, Queue<byte[]>> cached = logCache.getIfPresent(step.getJobId());

        if (Objects.isNull(cached)) {
            return Collections.emptyList();
        }

        return cached.get(stepId);
    }

    private Pathable[] getLogDir(String cmdId) {
        Step step = stepService.get(cmdId);

        return new Pathable[]{
                Flow.path(step.getFlowId()),
                Job.path(step.getBuildNumber()),
                LogPath
        };
    }

    private String getLogFile(String cmdId) {
        return cmdId + ".log";
    }
}
