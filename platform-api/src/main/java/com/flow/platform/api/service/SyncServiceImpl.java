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
import com.flow.platform.api.domain.sync.SyncTask;
import com.flow.platform.api.domain.sync.SyncType;
import com.flow.platform.api.service.job.CmdService;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.queue.PlatformQueue;
import com.flow.platform.util.Logger;
import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.JGitUtil;
import com.flow.platform.util.http.HttpURL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.annotation.PostConstruct;
import org.eclipse.jgit.lib.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */

@Service
public class SyncServiceImpl implements SyncService {

    private final static Logger LOGGER = new Logger(SyncService.class);

    private final Map<AgentPath, PlatformQueue<PriorityMessage>> syncQueue = new ConcurrentHashMap<>();

    private final Map<AgentPath, SyncTask> syncTasks = new ConcurrentHashMap<>();

    @Autowired
    private QueueCreator syncQueueCreator;

    @Autowired
    private GitService gitService;

    @Autowired
    private CmdService cmdService;

    @Value("${domain.api}")
    private String apiDomain;

    private String callbackUrl;

    @PostConstruct
    private void init() {
        callbackUrl = HttpURL.build(apiDomain).append("/hooks/sync").toString();
    }

    @Override
    public void put(SyncEvent event) {
        for (PlatformQueue<PriorityMessage> agentQueue : syncQueue.values()) {
            agentQueue.enqueue(PriorityMessage.create(event.toBytes(), DEFAULT_SYNC_QUEUE_PRIORITY));
        }
    }

    @Override
    public PlatformQueue<PriorityMessage> getSyncQueue(AgentPath agent) {
        return syncQueue.get(agent);
    }

    @Override
    public SyncTask getSyncTask(AgentPath agent) {
        return syncTasks.get(agent);
    }

    @Override
    public void register(AgentPath agent) {
        if (syncQueue.containsKey(agent)) {
            return;
        }

        PlatformQueue<PriorityMessage> queue = syncQueueCreator.create(agent.toString() + "-sync");
        syncQueue.put(agent, queue);

        // init sync event from git
        List<SyncEvent> syncEvents = initSyncEventFromGitWorkspace();
        for (SyncEvent event : syncEvents) {
            queue.enqueue(PriorityMessage.create(event.toBytes(), DEFAULT_SYNC_QUEUE_PRIORITY));
        }
    }

    @Override
    public void remove(AgentPath agent) {
        syncQueue.remove(agent);
        syncTasks.remove(agent);
    }

    @Override
    public void clean() {
        syncQueue.clear();
    }

    @Override
    public void onCallback(Cmd cmd) {
        SyncTask task = syncTasks.get(cmd.getAgentPath());

        // delete session if sync task for agent cannot be found
        if (cmd.getType() != CmdType.DELETE_SESSION && task == null) {
            CmdInfo deleteSession = new CmdInfo(cmd.getAgentPath(), CmdType.DELETE_SESSION, null);
            deleteSession.setWebhook(callbackUrl);
            deleteSession.setSessionId(cmd.getSessionId());
            cmdService.sendCmd(deleteSession, false, 0);
            return;
        }

        if (cmd.getType() == CmdType.DELETE_SESSION) {
            syncTasks.remove(cmd.getAgentPath());
            LOGGER.trace("Sync task finished for agent " + cmd.getAgentPath());
            return;
        }

        SyncEvent next = null;

        if (cmd.getType() == CmdType.CREATE_SESSION) {
            if (cmd.getStatus() == CmdStatus.SENT) {
                // get next sync event but not remove
                next = task.getSyncQueue().peek();
            }

            else {
                syncTasks.remove(cmd.getAgentPath());
                LOGGER.trace("Sync task stopped since create session failure for agent: " + cmd.getAgentPath());
                return;
            }
        }

        else if (cmd.getType() == CmdType.RUN_SHELL) {
            if (Cmd.FINISH_STATUS.contains(cmd.getStatus())) {
                CmdResult result = cmd.getCmdResult();

                boolean shouldSendBack = false;
                if (result == null) {
                    shouldSendBack = true;
                }

                else if (result.getExitValue() == null) {
                    shouldSendBack = true;
                }

                else if (result.getExitValue() != 0) {
                    shouldSendBack = true;
                }

                SyncEvent current = task.getSyncQueue().peek();
                if (shouldSendBack) {
                    syncQueue.get(cmd.getAgentPath())
                        .enqueue(PriorityMessage.create(current.toBytes(), DEFAULT_SYNC_QUEUE_PRIORITY));
                }

                // remove current sync event
                task.getSyncQueue().remove();

                // set next node whatever the result
                next = task.getSyncQueue().peek();
            }
        }

        // run next sync event
        if (next != null) {
            CmdInfo runShell = new CmdInfo(cmd.getAgentPath(), CmdType.RUN_SHELL, next.toScript());
            runShell.setWebhook(callbackUrl);
            runShell.setSessionId(cmd.getSessionId());
            runShell.setWorkingDir(DEFAULT_CMD_DIR);
            cmdService.sendCmd(runShell, false, 0);
        }

        // delete session when queue is empty
        if (next == null && task.getSyncQueue().isEmpty()) {
            CmdInfo deleteSession = new CmdInfo(cmd.getAgentPath(), CmdType.DELETE_SESSION, null);
            deleteSession.setWebhook(callbackUrl);
            deleteSession.setSessionId(cmd.getSessionId());
            cmdService.sendCmd(deleteSession, false, 0);
        }
    }

    @Override
    public void sync(AgentPath agentPath) {
        PlatformQueue<PriorityMessage> queue = syncQueue.get(agentPath);

        if (agentPath == null || queue.size() == 0) {
            return;
        }

        // create queue for agent task
        SyncTask task = new SyncTask(agentPath, buildSyncEventQueueForTask(queue));
        syncTasks.put(agentPath, task);

        // create cmd to create sync session with higher priority then job, the extra field record node path
        try {
            CmdInfo cmdInfo = new CmdInfo(agentPath, CmdType.CREATE_SESSION, null);
            cmdInfo.setWebhook(callbackUrl);
            cmdService.sendCmd(cmdInfo, true, DEFAULT_CMD_PRIORITY);
            LOGGER.trace("Start sync '%s' git repo to agent '%s'", task.getTotal(), agentPath);
        } catch (Throwable e) {
            syncTasks.remove(agentPath);
            LOGGER.warn(e.getMessage());
        }
    }

    @Override
    @Scheduled(fixedDelay = 60 * 1000 * 30, initialDelay = 60 * 1000)
    public void syncTask() {
        for (AgentPath agentPath : syncQueue.keySet()) {
            sync(agentPath);
        }
    }

    private Queue<SyncEvent> buildSyncEventQueueForTask(PlatformQueue<PriorityMessage> agentSyncQueue) {
        Queue<SyncEvent> syncEventQueue = new ConcurrentLinkedQueue<>();

        PriorityMessage message;
        while ((message = agentSyncQueue.dequeue()) != null) {
            SyncEvent event = SyncEvent.parse(message.getBody(), SyncEvent.class);
            syncEventQueue.add(event);
        }

        return syncEventQueue;
    }

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
