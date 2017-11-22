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

package com.flow.platform.api.service;

import com.flow.platform.api.domain.sync.SyncEvent;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.queue.PlatformQueue;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */

@Service
public class SyncServiceImpl implements SyncService {

    private final Map<AgentPath, PlatformQueue<PriorityMessage>> agentSyncQueue = new ConcurrentHashMap<>();

    @Autowired
    private QueueCreator syncQueueCreator;

    @Override
    public void put(SyncEvent event) {
        for (PlatformQueue<PriorityMessage> agentQueue : agentSyncQueue.values()) {
            agentQueue.enqueue(PriorityMessage.create(event.toBytes(), DEFAULT_SYNC_QUEUE_PRIORITY));
        }
    }

    @Override
    public PlatformQueue<PriorityMessage> get(AgentPath agent) {
        return agentSyncQueue.get(agent);
    }

    @Override
    public void register(AgentPath agent) {
        if (agentSyncQueue.containsKey(agent)) {
            return;
        }

        PlatformQueue<PriorityMessage> queue = syncQueueCreator.create(agent.toString() + "-sync-queue");
        agentSyncQueue.put(agent, queue);

        // init sync event from git
    }

    @Override
    public void remove(AgentPath agent) {
        agentSyncQueue.remove(agent);
    }

    /**
     * Init
     */
    private void initFormGitWorkspace() {

    }
}
