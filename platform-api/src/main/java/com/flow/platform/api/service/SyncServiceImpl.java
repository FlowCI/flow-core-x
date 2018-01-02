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

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.domain.sync.Sync;
import com.flow.platform.api.domain.sync.SyncEvent;
import com.flow.platform.api.domain.sync.SyncRepo;
import com.flow.platform.api.domain.sync.SyncTask;
import com.flow.platform.api.domain.sync.SyncType;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.service.job.CmdService;
import com.flow.platform.domain.Agent;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.AgentStatus;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdInfo;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.CommandUtil.Unix;
import com.flow.platform.util.Logger;
import com.flow.platform.util.StringUtil;
import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.JGitUtil;
import com.flow.platform.util.http.HttpURL;
import com.google.common.base.Strings;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.annotation.PostConstruct;
import org.eclipse.jgit.lib.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */

@Service
public class SyncServiceImpl implements SyncService {

    private final static Logger LOGGER = new Logger(SyncService.class);

    private final Map<AgentPath, Sync> syncs = new ConcurrentHashMap<>();

    private final Map<AgentPath, SyncTask> syncTasks = new ConcurrentHashMap<>();

    @Autowired
    private QueueCreator syncQueueCreator;

    @Autowired
    private GitService gitService;

    @Autowired
    private CmdService cmdService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Value("${domain.api}")
    private String apiDomain;

    private Set<SyncRepo> repos = Collections.emptySet();

    private String callbackUrl;

    @PostConstruct
    private void init() {
        callbackUrl = HttpURL.build(apiDomain).append("/hooks/sync").toString();

        taskExecutor.execute(() -> {
            try {
                LOGGER.trace("Start to init agent list in thread: " + Thread.currentThread().getName());
                load();

                List<Agent> agents = agentService.list();
                for (Agent agent : agents) {
                    if (agent.getStatus() == AgentStatus.OFFLINE) {
                        continue;
                    }
                    register(agent.getPath());
                }
            } catch (Throwable e) {
                LOGGER.warn(e.getMessage());
            }
        });
    }

    @Override
    public void load() {
        final List<Repository> gitRepos = gitService.repos();
        repos = new HashSet<>(gitRepos.size());

        for (Repository repo : gitRepos) {
            try {
                List<String> tags = JGitUtil.tags(repo);
                String gitRepoName = repo.getDirectory().getName();

                // git repo needs tags
                if (tags.isEmpty()) {
                    LOGGER.warn("Git repo '%s' cannot be synced since missing tag", gitRepoName);
                    continue;
                }

                gitRepoName = StringUtil.trimEnd(gitRepoName, ".git");
                repos.add(new SyncRepo(gitRepoName, tags.get(0)));
            } catch (GitException e) {
                LOGGER.warn(e.getMessage());
            } finally {
                repo.close();
            }
        }
    }

    @Override
    public void put(SyncEvent event) {
        for (Sync syncForAgent : syncs.values()) {
            syncForAgent.enqueue(event, DEFAULT_SYNC_QUEUE_PRIORITY);
        }
    }

    @Override
    public void reset() {
        List<SyncEvent> events = toEvents(repos, SyncType.CREATE);
        events.add(0, SyncEvent.DELETE_ALL);

        Set<Sync> cleanSet = new HashSet<>(syncs.size());

        for (Sync syncForAgent : syncs.values()) {
            if (!cleanSet.contains(syncForAgent)) {
                syncForAgent.cleanQueue();
                cleanSet.add(syncForAgent);
            }

            for (SyncEvent event : events) {
                event.setGitUrl(createGitUrl(event.getRepo().getName())); // ensure git url is correct
                syncForAgent.enqueue(event, DEFAULT_SYNC_QUEUE_PRIORITY);
            }
        }
    }

    @Override
    public void put(String name, String tag, SyncType type) {
        SyncEvent event = new SyncEvent(createGitUrl(name), name, tag, type);
        put(event);
    }

    @Override
    public Sync get(AgentPath agent) {
        return syncs.get(agent);
    }

    @Override
    public SyncTask getSyncTask(AgentPath agent) {
        return syncTasks.get(agent);
    }

    @Override
    public void register(AgentPath agent) {
        if (syncs.containsKey(agent)) {
            return;
        }

        Sync sync = new Sync(agent, syncQueueCreator.create(agent.toString() + "-sync"));
        syncs.put(agent, sync);

        // init sync event from git
        for (SyncEvent event : toEvents(repos, SyncType.CREATE)) {
            sync.enqueue(event, DEFAULT_SYNC_QUEUE_PRIORITY);
        }
    }

    @Override
    public void remove(AgentPath agent) {
        syncTasks.remove(agent);
        syncs.remove(agent);
    }

    @Override
    public void clean() {
        syncs.clear();
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
            Sync sync = syncs.get(cmd.getAgentPath());
            if (sync != null) {
                sync.setSyncTime(ZonedDateTime.now());
            }

            LOGGER.trace("Sync task finished for agent " + cmd.getAgentPath());
            return;
        }

        SyncEvent next = null;

        if (cmd.getType() == CmdType.CREATE_SESSION) {
            if (cmd.getStatus() == CmdStatus.SENT) {
                // get next sync event but not remove
                next = task.getSyncQueue().peek();
            } else {
                syncTasks.remove(cmd.getAgentPath());
                LOGGER.trace("Sync task stopped since create session failure for agent: " + cmd.getAgentPath());
                return;
            }
        } else if (cmd.getType() == CmdType.RUN_SHELL) {
            if (Cmd.FINISH_STATUS.contains(cmd.getStatus())) {
                CmdResult result = cmd.getCmdResult();
                if (Objects.isNull(result)) {
                    result = CmdResult.EMPTY;
                }

                boolean shouldSendBack = false;
                if (Objects.isNull(result.getExitValue()) || result.getExitValue() != 0) {
                    shouldSendBack = true;
                }

                SyncEvent current = task.getSyncQueue().peek();

                Sync sync = syncs.get(cmd.getAgentPath());
                if (sync != null) {
                    if (shouldSendBack) {
                        sync.enqueue(current, DEFAULT_SYNC_QUEUE_PRIORITY);
                    }

                    // update agent repo list from env FLOW_SYNC_LIST
                    String repos = result.getOutput().get(SyncEvent.FLOW_SYNC_LIST);
                    updateAgentRepo(sync, repos);
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
            runShell.setWorkingDir(AppConfig.DEFAULT_AGENT_REPO_DIR);
            runShell.setOutputEnvFilter(EnvUtil.parseCommaEnvToList(SyncEvent.FLOW_SYNC_LIST));
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
        Sync sync = syncs.get(agentPath);
        if (sync == null) {
            return;
        }

        // create queue for agent task
        SyncTask task = new SyncTask(agentPath, buildSyncEventQueueForTask(sync));
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
    @Scheduled(fixedDelay = 60 * 1000 * 10, initialDelay = 60 * 1000)
    public void syncTask() {
        for (AgentPath agentPath : syncs.keySet()) {
            sync(agentPath);
        }
    }

    /**
     * Update agent repo list
     *
     * @param sync Sync instance for agent
     * @param latestReposStr repo raw string, ex: RepoA[v1.0]\nRepoB[v1.0]
     */
    private void updateAgentRepo(Sync sync, String latestReposStr) {
        if (Strings.isNullOrEmpty(latestReposStr)) {
            sync.getRepos().clear();
            return;
        }

        String[] repos = latestReposStr.split(Unix.LINE_SEPARATOR);
        for (String repo : repos) {
            SyncRepo repoObj = SyncRepo.build(repo);
            if (Objects.isNull(repoObj)) {
                continue;
            }

            sync.getRepos().add(repoObj);
        }
    }

    /**
     * Build sync task from agent sync queue and list agent repos at the last
     */
    private Queue<SyncEvent> buildSyncEventQueueForTask(Sync sync) {
        Queue<SyncEvent> syncEventQueue = new ConcurrentLinkedQueue<>();

        SyncEvent event = null;
        while ((event = sync.dequeue()) != null) {
            syncEventQueue.add(event);
        }

        // list agent exist repos finally
        syncEventQueue.add(SyncEvent.LIST);
        return syncEventQueue;
    }

    private List<SyncEvent> toEvents(Collection<SyncRepo> repos, SyncType syncType) {
        List<SyncEvent> events = new LinkedList<>();
        for (SyncRepo repo : repos) {
            events.add(new SyncEvent(createGitUrl(repo.getName()), repo.getName(), repo.getTag(), syncType));
        }
        return events;
    }

    private String createGitUrl(String repoName) {
        return HttpURL.build(apiDomain).append("git").append(repoName + ".git").toString();
    }
}
