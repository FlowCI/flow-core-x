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

import com.flow.platform.api.domain.sync.Sync;
import com.flow.platform.api.domain.sync.SyncEvent;
import com.flow.platform.api.domain.sync.SyncTask;
import com.flow.platform.api.domain.sync.SyncType;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.queue.PlatformQueue;

/**
 * @author yang
 */

public interface SyncService {

    int DEFAULT_SYNC_QUEUE_PRIORITY = 1;

    int DEFAULT_CMD_PRIORITY = 10;

    String DEFAULT_CMD_DIR = "${HOME}/.flow-agent/repos";

    interface QueueCreator {

        PlatformQueue<PriorityMessage> create(String name);
    }

    /**
     * Put sync event to event queue
     */
    void put(SyncEvent event);

    /**
     * Put sync event with raw data
     * @param name repo name
     * @param tag repo tag
     * @param type sync type
     */
    void put(String name, String tag, SyncType type);

    /**
     * Reload sync event from git repo and reset all agent event queue
     */
    void reset();

    /**
     * Get sync data for agent
     */
    Sync get(AgentPath agent);

    /**
     * Get sync task tree for agent
     */
    SyncTask getSyncTask(AgentPath agent);

    /**
     * Register agent to sync service
     */
    void register(AgentPath agent);

    /**
     * Remove agent from sync service
     */
    void remove(AgentPath agent);

    /**
     * Clean agents
     */
    void clean();

    /**
     * Handle sync event cmd callback
     */
    void onCallback(Cmd cmd);

    /**
     * Start sync to agent
     * - do not execute if agent path has not been registered
     * - just list exiting repos if no other sync events
     */
    void sync(AgentPath agent);

    /**
     * Task to sync git repo to agents
     */
    void syncTask();

}
