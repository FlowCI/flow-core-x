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

/**
 * @author yang
 */

public interface SyncService {

    int DEFAULT_SYNC_QUEUE_PRIORITY = 1;

    interface QueueCreator {

        PlatformQueue<PriorityMessage> create(String name);
    }

    /**
     * Put sync event
     */
    void put(SyncEvent event);

    /**
     * Get sync event queue for agent
     */
    PlatformQueue<PriorityMessage> get(AgentPath agent);

    /**
     * Register agent to sync service
     */
    void register(AgentPath agent);

    /**
     * Remove agent from sync service
     */
    void remove(AgentPath agent);

}
