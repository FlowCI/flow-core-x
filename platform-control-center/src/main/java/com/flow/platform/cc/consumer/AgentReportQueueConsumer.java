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
import com.flow.platform.util.DateUtil;
import com.flow.platform.util.Logger;
import java.util.concurrent.BlockingQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent report queue consumer for in memory blocking queue
 *
 * @author yang
 */
@Component
public class AgentReportQueueConsumer extends QueueConsumerBase<AgentPath> {

    private final static Logger LOGGER = new Logger(AgentReportQueueConsumer.class);

    @Autowired
    private BlockingQueue<AgentPath> agentReportQueue;

    @Autowired
    private AgentDao agentDao;

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
        Agent exist = agentDao.find(key);

        // create new agent with idle status
        if (exist == null) {
            try {
                Agent agent = new Agent(key);
                agent.setCreatedDate(DateUtil.now());
                agent.setUpdatedDate(DateUtil.now());
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
