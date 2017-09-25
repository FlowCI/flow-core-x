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

package com.flow.platform.cc.consumer;

import com.flow.platform.cc.service.AgentService;
import com.flow.platform.core.consumer.QueueConsumer;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.util.Logger;
import java.util.concurrent.BlockingQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent report queue consumer for in memory blocking queue
 *
 * @author yang
 */
@Component
public class AgentReportQueueConsumer extends QueueConsumer<AgentPath> {

    private final static Logger LOGGER = new Logger(AgentReportQueueConsumer.class);

    @Autowired
    private BlockingQueue<AgentPath> agentReportQueue;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public ThreadPoolTaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    @Override
    public BlockingQueue<AgentPath> getQueue() {
        return agentReportQueue;
    }

    @Override
    public void onQueueItem(AgentPath path) {
        if (path == null) {
            return;
        }

        try {
            LOGGER.debug(Thread.currentThread().getName() + ": " + path.toString());
            reportOnline(path);
        } catch (Throwable e) {
            LOGGER.warn("Error on report online of agent %s info: %s", path, e.getMessage());
        }
    }

    /**
     * Find from online list and create
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void reportOnline(AgentPath key) {
        Agent exist = agentService.find(key);

        // create new agent with idle status
        if (exist == null) {
            try {
                exist = agentService.create(key, null);
                LOGGER.trace("Create agent %s from ReportOnline", key);
            } catch (DataIntegrityViolationException ignore) {
                // agent been created at some other threads
                return;
            }
        }

        // update exist offline agent to idle status
        if (exist.getStatus() == AgentStatus.OFFLINE) {
            agentService.saveWithStatus(exist, AgentStatus.IDLE);
        }
    }
}
