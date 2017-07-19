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

import com.flow.platform.cc.dao.AgentDao;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.util.Logger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
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
public class AgentReportQueueConsumer implements QueueConsumer {

    private final static Logger LOGGER = new Logger(AgentReportQueueConsumer.class);

    @Autowired
    private BlockingQueue<AgentPath> agentReportQueue;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    private volatile boolean shouldStop = false;

    @Override
    public String getName() {
        return "AgentReportQueueConsumer";
    }

    @Override
    public void start() {
        taskExecutor.execute(() -> {
            while (!shouldStop) {
                try {
                    AgentPath path = agentReportQueue.poll(1L, TimeUnit.SECONDS);
                    if (path != null) {
                        reportOnline(path);
                    }
                } catch (InterruptedException ignore) {
                    LOGGER.warn("InterruptedException when consuming agent report queue");
                }
            }
        });
    }

    @Override
    public void stop() {
        shouldStop = true;
    }

    /**
     * Find from online list and create
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void reportOnline(AgentPath key) {
        LOGGER.debug(Thread.currentThread().getName() + " : " + key.toString());
        Agent exist = agentDao.find(key);

        // create new agent with idle status
        if (exist == null) {
            try {
                Agent agent = new Agent(key);
                agent.setStatus(AgentStatus.IDLE);
                agentDao.save(agent);
            } catch (DataIntegrityViolationException ignore) {
                // agent been created at some other threads
            }
            return;
        }

        // update exist offline agent to idle status
        if (exist.getStatus() == AgentStatus.OFFLINE) {
            exist.setStatus(AgentStatus.IDLE);
            agentDao.update(exist);
        }
    }
}
