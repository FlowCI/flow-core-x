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
import com.flowci.common.exception.DuplicateException;
import com.flowci.common.exception.NotFoundException;
import com.flowci.common.exception.StatusException;
import com.flowci.common.helper.ObjectsHelper;
import com.flowci.core.agent.dao.AgentDao;
import com.flowci.core.agent.dao.AgentProfileDao;
import com.flowci.core.agent.domain.*;
import com.flowci.core.agent.domain.Agent.Status;
import com.flowci.core.agent.event.*;
import com.flowci.core.agent.manager.AgentEventManager;
import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.domain.PushEvent;
import com.flowci.core.common.helper.CipherHelper;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.common.manager.SocketPushManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.manager.SpringTaskManager;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.flowci.core.job.event.NoIdleAgentEvent;
import com.flowci.tree.Selector;
import com.flowci.zookeeper.InterLock;
import com.flowci.zookeeper.ZookeeperClient;
import com.google.common.collect.Sets;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static com.flowci.core.agent.domain.Agent.Status.*;

/**
 * Manage agent from zookeeper nodes
 * - The ephemeral node present agent, path is /{root}/{agent id}
 * - The persistent node present agent of lock, path is /{root}/{agent id}-lock, managed by server side
 *
 * @author yang
 */
@Slf4j
@Service
public class AgentServiceImpl implements AgentService {

    private static final String FetchAgentLockKey = "fetch-agent";

    private static final int DefaultAgentLockTimeout = 20; // seconds

    private static final int MinIdleAgentPushBack = 2; // seconds

    private static final int MaxIdleAgentPushBack = 10; // seconds

    @Autowired
    private String topicForAgentProfile;

    @Autowired
    private AppProperties.Zookeeper zkProperties;

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private AgentProfileDao agentProfileDao;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private SpringTaskManager taskManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AgentEventManager agentEventManager;

    @Autowired
    private String idleAgentQueue;

    @Autowired
    private RabbitOperations idleAgentQueueManager;

    @Autowired
    private SocketPushManager socketPushManager;

    @PostConstruct
    public void initAgentStatus() {
        taskManager.run("init-agent-status", true, () -> {
            for (Agent agent : agentDao.findAll()) {
                if (agent.isStarting() || agent.isOffline()) {
                    continue;
                }

                agent.setStatus(OFFLINE);
                agentDao.save(agent);
            }
        });
    }

    @PostConstruct
    public void subscribeIdleAgentQueue() throws IOException {
        idleAgentQueueManager.startConsumer(idleAgentQueue, false, (header, body, envelope) -> {
            String agentId = new String(body);
            log.debug("Got an idle agent {}", agentId);

            Agent agent = get(agentId);
            if (!agent.isIdle()) {
                log.debug("Agent {} is not idle", agentId);
                return true;
            }

            try {
                IdleAgentEvent event = new IdleAgentEvent(this, agentId);
                eventManager.publish(event);

                // agent not used after event, push back to queue
                Boolean shouldPushBack = event.getFetched();
                if (shouldPushBack) {
                    int randomSec = ObjectsHelper.randomNumber(MinIdleAgentPushBack, MaxIdleAgentPushBack);
                    ThreadHelper.sleep(randomSec * 1000L);
                    idleAgentQueueManager.send(idleAgentQueue, agentId.getBytes());
                }
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
            return true;
        }, null);
    }

    @PostConstruct
    public void lockNodeCleanup() {
        List<String> children = zk.children(zkProperties.getAgentRoot());
        for (String path : children) {
            String agentId = Util.getAgentIdFromLockPath(path);
            Optional<Agent> optional = agentDao.findById(agentId);

            if (optional.isEmpty()) {
                try {
                    zk.delete(path, true);
                } catch (Throwable ignore) {
                }
            }
        }
    }

    //====================================================================
    //        %% Public Methods
    //====================================================================

    @Override
    public Agent get(String id) {
        Optional<Agent> optional = agentDao.findById(id);
        if (optional.isEmpty()) {
            throw new NotFoundException("Agent {0} does not existed", id);
        }
        return optional.get();
    }

    @Override
    public AgentProfile getProfile(String token) {
        Optional<AgentProfile> optional = agentProfileDao.findById(token);
        return optional.orElse(AgentProfile.EMPTY);
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
    public Iterable<Agent> list(Collection<String> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return agentDao.findAllById(ids);
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
    public Optional<Agent> acquire(String jobId, Selector selector, String agentId, boolean shouldIdle) {
        Optional<InterLock> lock = lock();
        if (!lock.isPresent()) {
            return Optional.empty();
        }

        try {
            Optional<Agent> optional = agentDao.findById(agentId);
            if (!optional.isPresent()) {
                return Optional.empty();
            }

            Agent agent = optional.get();
            if (shouldIdle && !agent.isIdle()) {
                return Optional.empty();
            }

            if (!agent.match(selector)) {
                return Optional.empty();
            }

            agent.setJobId(jobId);
            update(agent, BUSY);
            return optional;
        } finally {
            unlock(lock.get());
        }
    }

    @Override
    public Optional<Agent> acquire(String jobId, Selector selector) {
        Optional<InterLock> lock = lock();
        if (!lock.isPresent()) {
            throw new StatusException("Unable to get lock");
        }

        try {
            List<Agent> agents = agentDao.findAll(selector.getLabel(), Sets.newHashSet(IDLE));
            if (agents.isEmpty()) {
                eventManager.publish(new NoIdleAgentEvent(this, jobId, selector));
                return Optional.empty();
            }

            Agent agent = agents.get(0);
            agent.setJobId(jobId);
            update(agent, BUSY);

            return Optional.of(agent);
        } finally {
            unlock(lock.get());
        }
    }

    @Override
    public void release(Collection<String> ids) {
        Optional<InterLock> lock = lock();
        if (!lock.isPresent()) {
            throw new StatusException("Unable to get lock");
        }

        try {
            for (String agentId : ids) {
                Agent agent = get(agentId);
                agent.setJobId(null);

                switch (agent.getStatus()) {
                    case OFFLINE:
                        update(agent, OFFLINE);
                    case BUSY:
                        update(agent, IDLE);
                        idleAgentQueueManager.send(idleAgentQueue, agentId.getBytes());
                }
            }
        } finally {
            unlock(lock.get());
        }
    }

    @Override
    public Agent create(AgentOption option) {
        String name = option.getName();

        Agent exist = agentDao.findByName(name);
        if (exist != null) {
            throw new DuplicateException("Agent name {0} is already defined", name);
        }

        try {
            // create agent
            Agent agent = new Agent(name, option.getTags());
            agent.setToken(UUID.randomUUID().toString());
            agent.setHostId(option.getHostId());
            agent.setExitOnIdle(option.getExitOnIdle());

            String dummyEmailForAgent = "agent." + name + "@flow.ci";
            agent.setRsa(CipherHelper.RSA.gen(dummyEmailForAgent));
            agentDao.insert(agent);

            eventManager.publish(new AgentCreatedEvent(this, agent));
            return agent;
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Agent name {0} is already defined", name);
        }
    }

    @Override
    public Agent update(AgentOption option) {
        Agent agent = getByToken(option.getToken());
        agent.setName(option.getName());
        agent.setTags(option.getTags());
        agent.setExitOnIdle(option.getExitOnIdle());

        try {
            return agentDao.save(agent);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Agent name {0} is already defined", option.getName());
        }
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
            log.warn("Unable to write CmdIn", e);
        }
    }

    //====================================================================
    //        %% Spring Event Listener
    //====================================================================

    @EventListener
    public void onConnected(OnConnectedEvent event) {
        Optional<InterLock> lock = lock();
        if (!lock.isPresent()) {
            log.error("Agent lock not available");
            return;
        }

        try {
            Agent target = getByToken(event.getToken());
            AgentInit init = event.getInit();

            target.setK8sCluster(init.getIsK8sCluster());
            target.setDocker(init.getIsDocker());
            target.setUrl("http://" + init.getIp() + ":" + init.getPort());
            target.setOs(init.getOs());
            target.setConnectedAt(Instant.now());

            update(target, init.getStatus());

            if (target.isIdle() && event.isToIdleQueue()) {
                idleAgentQueueManager.send(idleAgentQueue, target.getId().getBytes());
            }

            event.setAgent(target);
        } finally {
            unlock(lock.get());
        }
    }

    @EventListener
    public void onProfileReceived(OnAgentProfileEvent event) {
        agentProfileDao.save(event.getProfile());
        socketPushManager.push(topicForAgentProfile, PushEvent.STATUS_CHANGE, event.getProfile());
    }

    @EventListener
    public void onDisconnected(OnDisconnectedEvent event) {
        Optional<InterLock> lock = lock();
        try {
            Agent target = getByToken(event.getToken());
            update(target, OFFLINE);
        } catch (NotFoundException ignore) {

        } finally {
            lock.ifPresent(this::unlock);
        }
    }

    //====================================================================
    //        %% Private methods
    //====================================================================

    private Optional<InterLock> lock() {
        String path = zk.makePath("/agent-locks", FetchAgentLockKey);
        Optional<InterLock> lock = zk.lock(path, DefaultAgentLockTimeout);
        lock.ifPresent(interLock -> log.debug("Lock: {}", FetchAgentLockKey));
        return lock;
    }

    private void unlock(InterLock lock) {
        try {
            zk.release(lock);
            log.debug("Unlock: {}", FetchAgentLockKey);
        } catch (Exception warn) {
            log.warn("Unable to unlock agent", warn);
        }
    }
}
