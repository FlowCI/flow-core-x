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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.agent.dao.AgentDao;
import com.flowci.core.agent.domain.AgentInit;
import com.flowci.core.agent.event.AgentStatusEvent;
import com.flowci.core.agent.event.CmdSentEvent;
import com.flowci.core.agent.event.CreateAgentEvent;
import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.common.helper.CipherHelper;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.RabbitChannelOperation;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.NoIdleAgentEvent;
import com.flowci.core.job.event.StopJobConsumerEvent;
import com.flowci.domain.Agent;
import com.flowci.domain.Agent.Status;
import com.flowci.domain.CmdIn;
import com.flowci.domain.Settings;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import com.flowci.tree.Selector;
import com.flowci.util.ObjectsHelper;
import com.flowci.zookeeper.ZookeeperClient;
import com.flowci.zookeeper.ZookeeperException;
import com.google.common.collect.ImmutableSet;
import lombok.extern.log4j.Log4j2;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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

    private static final String LockPathSuffix = "-lock";

    private static final long RetryIntervalOnNotFound = 10 * 1000; // 10 seconds

    @Autowired
    private ConfigProperties.Zookeeper zkProperties;

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private RabbitChannelOperation agentQueueManager;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private Settings baseSettings;

    @Autowired
    private ObjectMapper objectMapper;

    // key is flow id,
    private final Map<String, AcquireLock> acquireLocks = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        initRootNode();
        initAgentsFromZk();
    }

    //====================================================================
    //        %% Public Methods
    //====================================================================

    @Override
    public Settings connect(AgentInit init) {
        Agent target = getByToken(init.getToken());
        target.setUrl("http://" + init.getIp() + ":" + init.getPort());
        target.setOs(init.getOs());
        target.setResource(init.getResource());
        agentDao.save(target);

        Settings settings = ObjectsHelper.copy(baseSettings);
        settings.setAgent(target);
        return settings;
    }

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
    public String getPath(Agent agent) {
        String root = zkProperties.getAgentRoot();
        return root + Agent.PATH_SLASH + agent.getId();
    }

    @Override
    public List<Agent> find(Status status, Set<String> tags) {
        List<Agent> agents;

        if (Objects.isNull(tags) || tags.isEmpty()) {
            agents = agentDao.findAllByStatus(status);
        } else {
            agents = agentDao.findAllByStatusAndTagsIn(status, tags);
        }

        return agents;
    }

    @Override
    public Agent delete(String token) {
        Agent agent = getByToken(token);
        agentDao.delete(agent);
        log.debug("{} has been deleted", agent);
        return agent;
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

            synchronized (lock) {
                log.debug("Job {} is waiting for agent", jobId);
                ThreadHelper.wait(lock, RetryIntervalOnNotFound);
            }
        }
    }

    @Override
    public Optional<Agent> tryLock(String jobId, String agentId) {
        // check agent is available form db
        Agent agent = get(agentId);

        if (agent.isBusy()) {
            return Optional.empty();
        }

        try {
            // check agent status from zk
            Status status = getStatusFromZk(agent);
            if (status != Status.IDLE) {
                return Optional.empty();
            }

            // lock and set status to busy
            agent.setJobId(jobId);
            String zkLockPath = getLockPath(agent);
            zk.lock(zkLockPath, path -> updateAgentStatus(agent, Status.BUSY));
            return Optional.of(agent);
        } catch (ZookeeperException e) {
            log.debug(e);
            return Optional.empty();
        }
    }

    @Override
    public void tryRelease(String agentId) {
        Agent agent = get(agentId);
        if (agent.isIdle()) {
            return;
        }

        agent.setJobId(null);
        updateAgentStatus(agent, Status.IDLE);
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
            agentQueueManager.declare(agent.getQueueName(), false);
            return agent;
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Agent name {0} is already defined", name);
        } catch (IOException e) {
            log.warn("Unable to declare agent queue, cause {}", e.getMessage());
            return agent;
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
    public void dispatch(CmdIn cmd, Agent agent) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(cmd);
            agentQueueManager.send(agent.getQueueName(), body);
            eventManager.publish(new CmdSentEvent(this, agent, cmd));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    //====================================================================
    //        %% Spring Event Listener
    //====================================================================

    @EventListener
    public void onCreateAgentEvent(CreateAgentEvent event) {
        Agent agent = this.create(event.getName(), event.getTags(), Optional.of(event.getHostId()));
        event.setCreated(agent);
    }

    @EventListener
    public void notifyToFindAvailableAgent(AgentStatusEvent event) {
        Agent agent = event.getAgent();

        if (!agent.hasJob()) {
            return;
        }

        if (!agent.isIdle()) {
            return;
        }

        // notify all consumer to find agent
        acquireLocks.computeIfPresent(agent.getJobId(), (s, lock) -> {
            synchronized (lock) {
                lock.notifyAll();
            }
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
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    //====================================================================
    //        %% Private methods
    //====================================================================

    /**
     * Get agent id from zookeeper path
     * <p>
     * Ex: /agents/123123, should get 123123
     */
    private static String getAgentIdFromPath(String path) {
        int index = path.lastIndexOf(Agent.PATH_SLASH);
        return path.substring(index + 1);
    }

    private void initRootNode() {
        String root = zkProperties.getAgentRoot();

        try {
            zk.create(CreateMode.PERSISTENT, root, null);
        } catch (ZookeeperException ignore) {

        }

        try {
            zk.watchChildren(root, new RootNodeListener());
        } catch (ZookeeperException e) {
            log.error(e.getMessage());
        }
    }

    private void initAgentsFromZk() {
        for (Agent agent : agentDao.findAll()) {
            String zkPath = getPath(agent);
            String zkLockPath = getLockPath(agent);

            // set to offline if zk node not exist
            if (!zk.exist(zkPath)) {
                agent.setStatus(Status.OFFLINE);
                agentDao.save(agent);
                zk.delete(zkLockPath, false);
                continue;
            }

            // sync status and lock node
            Status status = getStatusFromZk(agent);
            agent.setStatus(status);
            agentDao.save(agent);
            syncLockNode(agent, Type.CHILD_ADDED);
        }
    }

    /**
     * Update agent status from ZK and DB
     *
     * @param agent  target agent
     * @param status new status
     */
    private void updateAgentStatus(Agent agent, Status status) {
        if (agent.getStatus() == status) {
            log.debug("Do not update agent {} status since status the same", agent);
            return;
        }

        agent.setStatus(status);

        try {
            // try update zookeeper status if new status not same with zk
            Status current = getStatusFromZk(agent);
            if (current != status) {
                zk.set(getPath(agent), status.getBytes());
            }
        } catch (ZookeeperException e) {
            // set agent to offline when zk exception
            agent.setStatus(Status.OFFLINE);
            log.warn("Unable to update status on zk node: {}", e.getMessage());
        } finally {
            agentDao.save(agent);
            eventManager.publish(new AgentStatusEvent(this, agent));
        }
    }

    private void syncLockNode(Agent agent, Type type) {
        String lockPath = getLockPath(agent);

        if (type == Type.CHILD_ADDED || type == Type.CHILD_UPDATED || type == Type.CONNECTION_RECONNECTED) {
            try {
                zk.create(CreateMode.PERSISTENT, lockPath, null);
            } catch (Throwable ignore) {

            }
            return;
        }

        if (type == Type.CHILD_REMOVED) {
            try {
                zk.delete(lockPath, true);
            } catch (Throwable ignore) {

            }
        }
    }

    private String getLockPath(Agent agent) {
        return getPath(agent) + LockPathSuffix;
    }

    private Status getStatusFromZk(Agent agent) {
        byte[] statusInBytes = zk.get(getPath(agent));
        return Status.fromBytes(statusInBytes);
    }

    private Optional<Agent> acquire(String jobId, Selector selector) {
        List<Agent> agents = find(Agent.Status.IDLE, selector.getTags());

        if (agents.isEmpty()) {
            return Optional.empty();
        }

        Iterator<Agent> availableList = agents.iterator();

        // try to lock it
        while (availableList.hasNext()) {
            Agent agent = availableList.next();
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

    private class AcquireLock {

        private boolean stop = false;

    }

    private class RootNodeListener implements PathChildrenCacheListener {

        private final Set<Type> ChildOperations = ImmutableSet.of(
                Type.CHILD_ADDED,
                Type.CHILD_REMOVED,
                Type.CHILD_UPDATED
        );

        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) {
            if (ChildOperations.contains(event.getType())) {
                handleAgentStatusChange(event);
            }
        }

        private void handleAgentStatusChange(PathChildrenCacheEvent event) {
            String path = event.getData().getPath();

            // TODO: handle status change only within one node

            // do not handle event from lock node
            if (path.endsWith(LockPathSuffix)) {
                log.debug("Lock node '{}' event '{}' received", path, event.getType());
                return;
            }

            String agentId = getAgentIdFromPath(path);
            Agent agent = get(agentId);

            if (event.getType() == Type.CHILD_ADDED) {
                syncLockNode(agent, Type.CHILD_ADDED);
                updateAgentStatus(agent, Status.IDLE);
                log.debug("Event '{}' of agent '{}' with status '{}'", event.getType(), agent.getName(), Status.IDLE);
                return;
            }

            if (event.getType() == Type.CHILD_REMOVED) {
                syncLockNode(agent, Type.CHILD_REMOVED);
                updateAgentStatus(agent, Status.OFFLINE);
                log.debug("Event '{}' of agent '{}' with status '{}'", event.getType(), agent.getName(),
                        Status.OFFLINE);
                return;
            }

            if (event.getType() == Type.CONNECTION_RECONNECTED) {
                Status status = getStatusFromZk(agent);
                updateAgentStatus(agent, status);
                log.debug("Event '{}' of agent '{}' with status '{}'", event.getType(), agent.getName(), status);
            }
        }
    }
}
