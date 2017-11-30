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

import com.flow.platform.cc.config.QueueConfig;
import com.flow.platform.cc.exception.AgentErr;
import com.flow.platform.cc.service.AgentService;
import com.flow.platform.cc.service.CmdDispatchService;
import com.flow.platform.cc.service.CmdService;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.core.util.ThreadUtil;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.queue.PlatformQueue;
import com.flow.platform.queue.QueueListener;
import com.flow.platform.util.Logger;
import com.flow.platform.util.zk.ZkException;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cmd queue consumer to handle cmd from RabbitMQ
 *
 * @author gy@fir.im
 */
@Component
public class CmdQueueConsumer implements QueueListener<PriorityMessage> {

    private final static Logger LOGGER = new Logger(CmdQueueConsumer.class);

    private final static long RETRY_WAIT_TIME = 1000; // in millis

    @Value("${queue.cmd.idle_agent.period}")
    private Integer idleAgentPeriod; // period for check idle agent in seconds

    @Value("${queue.cmd.idle_agent.timeout}")
    private Integer idleAgentTimeout; // timeout if no idle agent in seconds

    @Autowired
    private CmdService cmdService;

    @Autowired
    private CmdDispatchService cmdDispatchService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private PlatformQueue<PriorityMessage> cmdQueue;

    @PostConstruct
    public void init() {
        cmdQueue.register(this);
    }

    @Override
    public void onQueueItem(PriorityMessage message) {
        String cmdId = new String(message.getBody());
        LOGGER.trace("Receive a cmd queue item: %s", cmdId);

        Cmd cmd = cmdService.find(cmdId);

        try {
            cmdDispatchService.dispatch(cmd);
        } catch (IllegalParameterException e) {
            LOGGER.warn("Illegal cmd id: %s", e.getMessage());
        } catch (IllegalStatusException e) {
            LOGGER.warn("Illegal cmd status: %s", e.getMessage());
        } catch (AgentErr.NotAvailableException | AgentErr.NotFoundException | ZkException.NotExitException e) {
            if (cmd.getRetry() <= 0) {
                return;
            }

            cmd = cmdService.find(cmdId);

            // do not re-enqueue if cmd been stopped or killed
            if (cmd.getStatus() == CmdStatus.STOPPED || cmd.getStatus() == CmdStatus.KILLED) {
                return;
            }

            // reset cmd status to pending, record num of retry
            int retry = cmd.getRetry() - 1;
            cmd.setStatus(CmdStatus.PENDING);
            cmd.setRetry(retry);
            cmdService.save(cmd);

            // re-enqueue
            resend(cmd.getId(), retry);

        } catch (Throwable e) {
            LOGGER.error("Unexpected exception", e);
        }
    }

    /**
     * Re-enqueue cmd and return num of retry
     */
    private void resend(final String cmdId, final int retry) {
        if (retry <= 0) {
            return;
        }

        ThreadUtil.sleep(RETRY_WAIT_TIME);

        // reset cmd status
        PriorityMessage message = PriorityMessage.create(cmdId.getBytes(), QueueConfig.MAX_PRIORITY);
        cmdQueue.enqueue(message);
        LOGGER.trace("Re-enqueue item %s", cmdId);
    }
}