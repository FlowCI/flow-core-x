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
import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.domain.Agent.Status;
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
import com.flowci.core.common.rabbit.RabbitOperations;
import com.flowci.core.job.event.NoIdleAgentEvent;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.tree.Selector;
import com.flowci.zookeeper.InterLock;
import com.flowci.zookeeper.ZookeeperClient;
import com.google.common.collect.Sets;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

import static com.flowci.core.agent.domain.Agent.Status.*;

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

    private static final String FetchAgentLockKey = "fetch-agent";

    private static final int DefaultAgentLockTimeout = 20; // seconds

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

    @Autowired
    private String idleAgentQueue;

    @Autowired
    private RabbitOperations idleAgentQueueManager;

    @EventListener(ContextRefreshedEvent.class)
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

    @EventListener(ContextRefreshedEvent.class)
    public void subscribeIdleAgentQueue() throws IOException {
        idleAgentQueueManager.startConsumer(idleAgentQueue, false, (header, body, envelope) -> {
            String agentId = new String(body);
            log.debug("Got an idle agent {}", agentId);

            try {
                Agent agent = get(agentId);
                if (!agent.isIdle()) {
                    log.debug("Agent {} no longer idle", agentId);
                    return true;
                }

                eventManager.publish(new IdleAgentEvent(this, agent));

                // agent not used, push back to queue
                if (agent.isIdle()) {
                    ThreadHelper.sleep(5 * 1000);
                    idleAgentQueueManager.send(idleAgentQueue, agent.getId().getBytes());
                }

            } catch (Exception e) {
                log.warn(e.getMessage());
            }
            return true;
        }, null);
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
    public Iterable<Agent> list(Collection<String> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return agentDao.findAllById(ids);
    }

    @Override
    public List<Agent> find(Selector selector) {
        return agentDao.findAll(selector.getLabel(), null);
    }

    @Override
    public List<Agent> find(Selector selector, Status status) {
        return agentDao.findAll(selector.getLabel(), Sets.newHashSet(status));
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
    public Optional<Agent> acquire(String jobId, Selector selector) {
        Optional<InterLock> lock = lock();
        if (!lock.isPresent()) {
            throw new StatusException("Unable to get lock");
        }

        try {
            List<Agent> agents = find(selector, IDLE);
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
    public void assign(String jobId, Agent agent) {
        Optional<InterLock> lock = lock();
        if (!lock.isPresent()) {
            throw new StatusException("Unable to get lock");
        }

        try {
            agent.setJobId(jobId);
            update(agent, BUSY);
        } finally {
            unlock(lock.get());
        }
    }

    @Override
    public Agent create(String name, Set<String> tags, Optional<String> hostId) {
        Agent exist = agentDao.findByName(name);
        if (exist != null) {
            throw new DuplicateException("Agent name {0} is already defined", name);
        }

        try {
            // create agent
            Agent agent = new Agent(name, tags);
            agent.setToken(UUID.randomUUID().toString());
            hostId.ifPresent(agent::setHostId);

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

        if (target.isIdle()) {
            idleAgentQueueManager.send(idleAgentQueue, target.getId().getBytes());
        }
    }

    @EventListener
    public void onDisconnected(OnDisconnectedEvent event) {
        try {
            Agent target = getByToken(event.getToken());
            update(target, OFFLINE);
        } catch (NotFoundException ignore) {

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
            log.warn(warn);
        }
    }
}
