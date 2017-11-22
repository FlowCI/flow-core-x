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
import com.flow.platform.api.domain.sync.SyncType;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.queue.PlatformQueue;
import com.flow.platform.util.Logger;
import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.JGitUtil;
import com.flow.platform.util.http.HttpURL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.jgit.lib.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */

@Service
public class SyncServiceImpl implements SyncService {

    private final static Logger LOGGER = new Logger(SyncService.class);

    private final Map<AgentPath, PlatformQueue<PriorityMessage>> agentSyncQueue = new ConcurrentHashMap<>();

    @Autowired
    private QueueCreator syncQueueCreator;

    @Autowired
    private GitService gitService;

    @Value("${domain.api}")
    private String apiDomain;

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
        List<SyncEvent> syncEvents = initSyncEventFromGitWorkspace();
        for (SyncEvent event: syncEvents) {
            queue.enqueue(PriorityMessage.create(event.toBytes(), DEFAULT_SYNC_QUEUE_PRIORITY));
        }
    }

    @Override
    public void remove(AgentPath agent) {
        agentSyncQueue.remove(agent);
    }

    @Override
    public void clean() {
        agentSyncQueue.clear();
    }

    /**
     * Init
     */
    private List<SyncEvent> initSyncEventFromGitWorkspace() {
        List<Repository> repos = gitService.repos();
        List<SyncEvent> syncEvents = new ArrayList<>(repos.size());

        for (Repository repo : repos) {
            try {
                List<String> tags = JGitUtil.tags(repo);
                String gitRepoName = repo.getDirectory().getName();

                // git repo needs tags
                if (tags.isEmpty()) {
                    LOGGER.warn("Git repo '%s' cannot be synced since missing tag", gitRepoName);
                    continue;
                }

                HttpURL gitURL = HttpURL.build(apiDomain).append("git").append(gitRepoName);
                syncEvents.add(new SyncEvent(gitURL.toString(), tags.get(0), SyncType.CREATE));
            } catch (GitException e) {
                LOGGER.warn(e.getMessage());
            } finally {
                repo.close();
            }
        }

        return syncEvents;
    }
}
