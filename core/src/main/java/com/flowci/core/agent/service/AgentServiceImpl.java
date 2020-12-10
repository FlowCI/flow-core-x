/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.agent.dao.AgentDao;
import com.flowci.core.agent.domain.AgentInit;
import com.flowci.core.agent.domain.CmdIn;
import com.flowci.core.agent.domain.Util;
import com.flowci.core.agent.event.*;
import com.flowci.core.agent.manager.AgentEventManager;
import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.helper.CipherHelper;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.manager.SpringTaskManager;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.NoIdleAgentEvent;
import com.flowci.core.job.event.StopJobConsumerEvent;
import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.domain.Agent.Status;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import com.flowci.tree.Selector;
import com.flowci.util.ObjectsHelper;
import com.flowci.zookeeper.ZookeeperClient;
import com.flowci.zookeeper.ZookeeperException;
import lombok.extern.log4j.Log4j2;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static com.flowci.core.agent.domain.Agent.Status.IDLE;
import static com.flowci.core.agent.domain.Agent.Status.OFFLINE;

/**
 * Manage agent from zookeeper nodes
 * - The ephemeral node present agent, path is /{root}/{agent id}
 * - The persistent node present agent of lock, path is /{root}/{agent id}-lock, managed by server side
 *
 * @author yang
 */
@Log4j2
@Service
public class AgentServiceImpl implements AgentService {

    private static final long RetryIntervalOnNotFound = 10 * 1000; // 10 seconds

    @Autowired
    private AppProperties.Zookeeper zkProperties;

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private SpringTaskManager taskManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AgentEventManager agentEventManager;

    // key is flow id,
    private final Map<String, AcquireLock> acquireLocks = new ConcurrentHashMap<>();

    @PostConstruct
    public void initAgentStatus() {
        taskManager.run("init-agent-status", () -> {
            for (Agent agent : agentDao.findAll()) {
                if (agent.isStarting() || agent.isOffline()) {
                    continue;
                }

                agent.setStatus(OFFLINE);
                agentDao.save(agent);
            }
        });
    }

    //====================================================================
    //        %% Public Methods
    //====================================================================

    @Override
    public Agent get(String id) {
        Optional<Agent> optional = agentDao.findById(id);
        if (!optional.isPresent()) {
            throw new NotFoundException("Agent {0} does not existed", id);
        }
        return optional.get();
    }

    @Override
    public Agent getByName(String name) {
        Agent agent = agentDao.findByName(name);
        if (Objects.isNull(agent)) {
            throw new NotFoundException("Agent name {0} is not available", name);
        }
        return agent;
    }

    @Override
    public Agent getByToken(String token) {
        Agent agent = agentDao.findByToken(token);
        if (Objects.isNull(agent)) {
            throw new NotFoundException("Agent token {0} is not available", token);
        }
        return agent;
    }

    @Override
    public boolean isExisted(String token) {
        return agentDao.existsAgentByToken(token);
    }

    @Override
    public List<Agent> list() {
        return agentDao.findAll();
    }

    @Override
    public List<Agent> find(Set<String> tags) {
        if (ObjectsHelper.hasCollection(tags)) {
            return agentDao.findAllByTagsIn(tags);
        }

        return agentDao.findAll();
    }

    @Override
    public Agent delete(String token) {
        Agent agent = getByToken(token);
        delete(agent);
        return agent;
    }

    @Override
    public void delete(Agent agent) {
        agentDao.delete(agent);
        log.debug("{} has been deleted", agent.getName());
    }

    @Override
    public Agent setTags(String token, Set<String> tags) {
        Agent agent = getByToken(token);
        agent.setTags(tags);
        agentDao.save(agent);
        return agent;
    }

    @Override
    public Optional<Agent> acquire(Job job, Function<String, Boolean> canContinue) {
        String jobId = job.getId();
        Selector selector = job.getAgentSelector();

        for (; ; ) {
            if (!canContinue.apply(jobId)) {
                log.debug("Job {} cannot continue to wait agent", jobId);
                acquireLocks.remove(job.getFlowId());
                return Optional.empty();
            }

            Optional<Agent> optional = acquire(jobId, selector);
            if (optional.isPresent()) {
                acquireLocks.remove(job.getFlowId());
                return optional;
            }

            eventManager.publish(new NoIdleAgentEvent(this, jobId, selector));

            AcquireLock lock = acquireLocks.computeIfAbsent(job.getFlowId(), s -> new AcquireLock());
            if (lock.stop) {
                acquireLocks.remove(job.getFlowId());
                return Optional.empty();
            }

            log.debug("Job {} is waiting for agent", jobId);
            ThreadHelper.wait(lock, RetryIntervalOnNotFound);
        }
    }

    @Override
    public Optional<Agent> tryLock(String jobId, String agentId) {
        // check agent is available form db
        Agent agent = get(agentId);
        if (agent.isBusy()) {
            return Optional.empty();
        }

        // lock and set status to busy
        try {
            String zkLockPath = Util.getZkLockPath(zkProperties.getAgentRoot(), agent);
            zk.lock(zkLockPath, path -> {
                agent.setJobId(jobId);
                update(agent, Status.BUSY);
            });
            return Optional.of(agent);
        } catch (ZookeeperException e) {
            log.debug(e);
            return Optional.empty();
        }
    }

    @Override
    public void tryRelease(String agentId) {
        Agent agent = get(agentId);
        agent.setJobId(null);

        switch (agent.getStatus()) {
            case OFFLINE:
                update(agent, OFFLINE);
                return;
            case BUSY:
                update(agent, IDLE);
        }
    }

    @Override
    public Agent create(String name, Set<String> tags, Optional<String> hostId) {
        Agent exist = agentDao.findByName(name);
        if (exist != null) {
            throw new DuplicateException("Agent name {0} is already defined", name);
        }

        Agent agent = new Agent(name, tags);
        agent.setToken(UUID.randomUUID().toString());
        hostId.ifPresent(agent::setHostId);

        String dummyEmailForAgent = "agent." + name + "@flow.ci";
        agent.setRsa(CipherHelper.RSA.gen(dummyEmailForAgent));

        try {
            agentDao.insert(agent);
            eventManager.publish(new AgentCreatedEvent(this, agent));
            return agent;
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Agent name {0} is already defined", name);
        }
    }

    @Override
    public Agent update(String token, String name, Set<String> tags) {
        Agent agent = getByToken(token);
        agent.setName(name);
        agent.setTags(tags);

        try {
            return agentDao.save(agent);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Agent name {0} is already defined", name);
        }
    }

    @Override
    public Agent update(String token, Agent.Resource resource) {
        Agent agent = getByToken(token);
        agent.setResource(resource);
        agentDao.save(agent);
        return agent;
    }

    @Override
    public Agent update(Agent agent, Status status) {
        if (agent.getStatus() == status) {
            agentDao.save(agent);
            return agent;
        }

        agent.setStatus(status);
        agentDao.save(agent);

        eventManager.publish(new AgentStatusEvent(this, agent));
        return agent;
    }

    @Override
    public void dispatch(CmdIn cmd, Agent agent) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(cmd);
            agentEventManager.writeMessage(agent.getToken(), body);
            eventManager.publish(new CmdSentEvent(this, agent, cmd));
        } catch (IOException e) {
            log.warn(e);
        }
    }

    //====================================================================
    //        %% Spring Event Listener
    //====================================================================

    @EventListener(ContextRefreshedEvent.class)
    public void lockNodeCleanup() {
        List<String> children = zk.children(zkProperties.getAgentRoot());
        for (String path : children) {
            String agentId = Util.getAgentIdFromLockPath(path);
            Optional<Agent> optional = agentDao.findById(agentId);

            if (!optional.isPresent()) {
                try {
                    zk.delete(path, true);
                } catch (Throwable ignore) {
                }
            }
        }
    }

    @EventListener
    public void onConnected(OnConnectedEvent event) {
        Agent target = getByToken(event.getToken());
        AgentInit init = event.getInit();

        target.setK8sCluster(init.getK8sCluster());
        target.setUrl("http://" + init.getIp() + ":" + init.getPort());
        target.setOs(init.getOs());
        target.setResource(init.getResource());

        update(target, init.getStatus());
        syncLockNode(target, true);
    }

    @EventListener
    public void onDisconnected(OnDisconnectedEvent event) {
        try {
            Agent target = getByToken(event.getToken());
            update(target, OFFLINE);
            syncLockNode(target, false);
        } catch (NotFoundException ignore) {

        }
    }

    @EventListener
    public void notifyToFindAgent(AgentStatusEvent event) {
        Agent agent = event.getAgent();

        if (!agent.hasJob()) {
            return;
        }

        if (!agent.isIdle()) {
            return;
        }

        eventManager.publish(new AgentIdleEvent(this, agent));
    }

    @EventListener
    public void doNotifyToFindAgent(AgentIdleEvent event) {
        Agent agent = event.getAgent();
        acquireLocks.computeIfPresent(agent.getJobId(), (s, lock) -> {
            ThreadHelper.notifyAll(lock);
            return lock;
        });
    }

    @EventListener
    public void stopJobsThatWaitingForAgent(StopJobConsumerEvent event) {
        AcquireLock lock = acquireLocks.get(event.getFlowId());
        if (Objects.isNull(lock)) {
            return;
        }

        lock.stop = true;
        ThreadHelper.notifyAll(lock);
    }

    //====================================================================
    //        %% Private methods
    //====================================================================

    private void syncLockNode(Agent agent, boolean isCreate) {
        String lockPath = Util.getZkLockPath(zkProperties.getAgentRoot(), agent);

        if (isCreate) {
            try {
                zk.create(CreateMode.PERSISTENT, lockPath, null);
            } catch (Throwable ignore) {

            }
            return;
        }

        try {
            zk.delete(lockPath, true);
        } catch (Throwable ignore) {

        }
    }

    private Optional<Agent> acquire(String jobId, Selector selector) {
        List<Agent> agents = find(selector.getLabel());

        if (agents.isEmpty()) {
            return Optional.empty();
        }

        Iterator<Agent> availableList = agents.iterator();

        // try to lock it
        while (availableList.hasNext()) {
            Agent agent = availableList.next();
            if (!agent.isIdle()) {
                continue;
            }

            Optional<Agent> locked = tryLock(jobId, agent.getId());
            if (locked.isPresent()) {
                return locked;
            }

            availableList.remove();
        }

        return Optional.empty();
    }

    //====================================================================
    //        %% Inner classes
    //====================================================================

    private static class AcquireLock {

        private boolean stop = false;
    }
}
