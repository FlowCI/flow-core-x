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

package com.flow.platform.api.domain.sync;

import com.flow.platform.domain.AgentPath;
import java.util.Queue;

/**
 * @author yang
 */
public class SyncTask {

    /**
     * Agent path of sync task
     */
    private final AgentPath path;

    /**
     * Total num of repo to be sync
     */
    private final Integer total;

    /**
     * The sync event queue for this task
     */
    private final Queue<SyncEvent> syncQueue;

    public SyncTask(AgentPath path, Queue<SyncEvent> syncQueue) {
        this.path = path;
        this.total = syncQueue.size();
        this.syncQueue = syncQueue;
    }

    public AgentPath getPath() {
        return path;
    }

    public Integer getTotal() {
        return total;
    }

    public Queue<SyncEvent> getSyncQueue() {
        return syncQueue;
    }
}
