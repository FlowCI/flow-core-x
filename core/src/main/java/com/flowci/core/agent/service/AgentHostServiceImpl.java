/*
 * Copyright 2020 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.flowci.core.agent.service;

import com.flowci.core.agent.dao.AgentDao;
import com.flowci.core.agent.dao.AgentHostDao;
import com.flowci.core.agent.domain.AgentHost;
import com.flowci.core.agent.domain.K8sAgentHost;
import com.flowci.core.agent.domain.LocalUnixAgentHost;
import com.flowci.core.agent.domain.SshAgentHost;
import com.flowci.core.agent.event.AgentCreatedEvent;
import com.flowci.core.agent.event.AgentHostStatusEvent;
import com.flowci.core.agent.manager.AgentStatusManager;
import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.helper.CacheHelper;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.job.event.NoIdleAgentEvent;
import com.flowci.core.secret.domain.KubeConfigSecret;
import com.flowci.core.secret.domain.RSASecret;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.event.GetSecretEvent;
import com.flowci.core.user.domain.User;
import com.flowci.docker.ContainerManager;
import com.flowci.docker.DockerManager;
import com.flowci.docker.DockerSSHManager;
import com.flowci.docker.K8sManager;
import com.flowci.docker.domain.*;
import com.flowci.domain.Agent;
import com.flowci.exception.NotAvailableException;
import com.flowci.exception.NotFoundException;
import com.flowci.util.StringHelper;
import com.flowci.zookeeper.ZookeeperClient;
import com.flowci.zookeeper.ZookeeperException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static com.flowci.core.agent.domain.Variables.*;
import static com.flowci.core.secret.domain.Secret.Category.KUBE_CONFIG;
import static com.flowci.core.secret.domain.Secret.Category.SSH_RSA;

@Log4j2
@Service
public class AgentHostServiceImpl implements AgentHostService {

    private static final String DefaultImage = "flowci/agent:dev";

    private static final String DefaultWorkspace = "/ws";

    private static final String DockerSock = "/var/run/docker.sock";

    private static final String ContainerNamePrefix = "ci-agent";

    private final Map<Class<?>, HostAdaptor> mapping = new HashMap<>(3);

    private final Cache<AgentHost, DockerManager> poolManagerCache =
            CacheHelper.createLocalCache(10, 600, new PoolManagerRemover());

    private String collectTaskZkPath;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private AppProperties.K8s k8sProperties;

    @Autowired
    private String serverUrl;

    @Autowired
    private K8sAgentHost.Hosts k8sHosts;

    @Autowired
    private List<K8sAgentHost.Endpoint> k8sEndpoints;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private AgentHostDao agentHostDao;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private AgentStatusManager statusManager;

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    private AppProperties.Zookeeper zkProperties;

    @Autowired
    private TaskExecutor appTaskExecutor;

    @Autowired
    private DockerManager dockerManager;

    @Autowired
    private AgentService agentService;

    {
        mapping.put(LocalUnixAgentHost.class, new LocalSocketHostAdaptor());
        mapping.put(SshAgentHost.class, new SshHostAdaptor());
        mapping.put(K8sAgentHost.class, new K8sHostAdaptor());
    }

    //====================================================================
    //        %% Public functions
    //====================================================================

    @Override
    public void createOrUpdate(AgentHost host) {
        if (StringHelper.hasValue(host.getId())) {
            agentHostDao.save(host);
            poolManagerCache.invalidate(host);
            return;
        }

        mapping.get(host.getClass()).create(host);
    }

    @Override
    public void delete(AgentHost host) {
        agentHostDao.deleteById(host.getId());
        appTaskExecutor.execute(() -> {
            removeAll(host);
        });
    }

    @Override
    public List<AgentHost> list() {
        return agentHostDao.findAll();
    }

    @Override
    public AgentHost get(String name) {
        Optional<AgentHost> optional = agentHostDao.findByName(name);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("Agent host {0} not found", name);
    }

    @Override
    public void sync(AgentHost host) {
        Optional<DockerManager> optional = getDockerManager(host);
        if (!optional.isPresent()) {
            log.warn("Fail to get pool manager of host: {}", host.getName());
            return;
        }

        ContainerManager cm = optional.get().getContainerManager();
        List<Unit> containerList;

        try {
            containerList = cm.list(null, ContainerNamePrefix + "*");
        } catch (Exception e) {
            log.warn("Cannot list containers of host {}", host.getName());
            return;
        }

        Set<AgentItemWrapper> containerSet = AgentItemWrapper.toSet(containerList);
        List<Agent> agentList = agentDao.findAllByHostId(host.getId());
        Set<AgentItemWrapper> agentSet = AgentItemWrapper.toSet(agentList);

        // find and remove containers are not belong to host
        containerSet.removeAll(agentSet);

        for (AgentItemWrapper item : containerSet) {
            try {
                List<Unit> list = cm.list(null, item.getName());
                if (list.size() > 0) {
                    cm.delete(list.get(0).getId());
                }
                log.info("Agent {} has been cleaned up", item.getName());
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public boolean start(AgentHost host) {
        Optional<DockerManager> manager = getDockerManager(host);
        if (!manager.isPresent()) {
            log.warn("Fail to get pool manager of host: {}", host.getName());
            return false;
        }

        log.info("try to start agent from host {}", host.getName());

        List<Agent> agents = agentDao.findAllByHostId(host.getId());
        List<Agent> startList = new LinkedList<>();

        DockerManager dockerManager = manager.get();
        ContainerManager cm = dockerManager.getContainerManager();

        for (Agent agent : agents) {
            if (statusManager.get(agent) != Agent.Status.OFFLINE) {
                continue;
            }

            // try to resume if offline, add to start list if failed
            try {
                List<Unit> list = cm.list(null, getContainerName(agent));

                // container not exist
                if (list.isEmpty()) {
                    startList.add(agent);
                    continue;
                }

                Unit container = list.get(0);
                cm.resume(container.getId());
                log.info("Agent {} been resumed", agent.getName());
                return true;
            } catch (Exception e) {
                log.warn("Unable to resume agent {}", agent.getName());
                startList.add(agent);
            }
        }

        // start from offline, and delete if cannot be started
        for (Agent agent : startList) {
            try {
                StartOption startOption = mapping.get(host.getClass()).buildStartOption(host, agent);
                cm.start(startOption);
                log.info("Agent {} been started", agent.getName());
                return true;
            } catch (Exception e) {
                log.warn("Unable to restart agent {}", agent.getName());

                //TODO: send notification
                agentService.delete(agent);
                agents.remove(agent);
            }
        }

        // create new agent
        if (agents.size() < host.getMaxSize()) {
            String random = StringHelper.randomString(5);
            String name = String.format("%s-%s", host.getName(), random);

            Agent agent = null;
            try {
                agent = agentService.create(name, host.getTags(), Optional.of(host.getId()));
                StartOption startOption = mapping.get(host.getClass()).buildStartOption(host, agent);
                cm.start(startOption);
                eventManager.publish(new AgentCreatedEvent(this, agent, host));
                log.info("Agent {} been created and started", name);
                return true;
            } catch (Exception e) {
                if (agent != null) {
                    agentService.delete(agent);
                    log.warn("Unable to start created agent {}, since {}", agent.getName(), e.getMessage());
                }
                return false;
            }
        }

        log.warn("Unable to start agent since over the limit size {}", host.getMaxSize());
        return false;
    }

    @Override
    public int size(AgentHost host) {
        Optional<DockerManager> optional = getDockerManager(host);
        if (!optional.isPresent()) {
            log.warn("Fail to get pool manager of host: {}", host.getName());
            return -1;
        }

        try {
            return optional.get().getContainerManager().list(null, ContainerNamePrefix + "*").size();
        } catch (Exception e) {
            log.warn("Cannot get container size of host {}", host.getName());
            return -1;
        }
    }

    @Override
    public void testConn(AgentHost host) {
        appTaskExecutor.execute(() -> getDockerManager(host));
    }

    @Override
    public void collect(AgentHost host) {
        // TODO: not implemented
    }

    @Override
    public void removeAll(AgentHost host) {
        List<Agent> list = agentDao.findAllByHostId(host.getId());
        for (Agent agent : list) {
            agentDao.delete(agent);
        }

        Optional<DockerManager> optional = getDockerManager(host);
        if (!optional.isPresent()) {
            log.warn("Fail to get pool manager of host: {}", host.getName());
            return;
        }

        ContainerManager cm = optional.get().getContainerManager();
        try {
            List<Unit> containers = cm.list(null, ContainerNamePrefix + "*");
            for (Unit c : containers) {
                cm.delete(c.getId());
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    public void scheduleCollect() {
        try {
            if (!lock()) {
                return;
            }

            log.info("Start to collect agents from host");
            for (AgentHost host : list()) {
                collect(host);
            }
            log.info("Collection finished");
        } finally {
            clean();
        }
    }

    //====================================================================
    //        %% Internal events
    //====================================================================

    @EventListener
    public void onContextReady(ContextRefreshedEvent event) {
        initZkNodeForCronTask();
        autoCreateLocalAgentHost();
        syncAgents();
    }

    @EventListener
    public void onNoIdleAgent(NoIdleAgentEvent event) {
        Set<String> agentTags = event.getSelector().getLabel();
        List<AgentHost> hosts = agentTags.isEmpty() ? list() : agentHostDao.findAllByTagsIn(agentTags);

        if (hosts.isEmpty()) {
            log.warn("Unable to find matched agent host for job {}", event.getJobId());
            return;
        }

        for (AgentHost host : hosts) {
            if (start(host)) {
                return;
            }
        }
    }

    //====================================================================
    //        %% Private functions
    //====================================================================

    public String getContainerName(Agent agent) {
        return String.format("%s-%s", ContainerNamePrefix, StringHelper.escapeNumber(agent.getName()));
    }

    private void initZkNodeForCronTask() {
        collectTaskZkPath = ZKPaths.makePath(zkProperties.getCronRoot(), "agent-host-collect");
    }

    public void autoCreateLocalAgentHost() {
        if (!appProperties.isAutoLocalAgentHost()) {
            return;
        }

        try {
            LocalUnixAgentHost host = new LocalUnixAgentHost();
            host.setName("localhost");
            createOrUpdate(host);
            log.info("Local unix agent host been created");
        } catch (NotAvailableException e) {
            log.warn(e.getMessage());
        }
    }

    public void syncAgents() {
        for (AgentHost host : list()) {
            try {
                sync(host);
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        }
    }

    /**
     * Load or init pool manager from local cache for each agent host
     */
    private Optional<DockerManager> getDockerManager(AgentHost host) {
        DockerManager manager = poolManagerCache.get(host, (h) -> {
            try {
                return mapping.get(host.getClass()).init(host);
            } catch (Exception e) {
                log.warn(e.getMessage());
                host.setError(e.getMessage());
            }
            return null;
        });

        if (Objects.isNull(manager)) {
            updateAgentHostStatus(host, AgentHost.Status.Disconnected);
            return Optional.empty();
        }

        host.setError(null);
        updateAgentHostStatus(host, AgentHost.Status.Connected);
        return Optional.of(manager);
    }

    private void updateAgentHostStatus(AgentHost host, AgentHost.Status newStatus) {
        if (!agentHostDao.existsById(host.getId())) {
            return;
        }

        host.setStatus(newStatus);
        agentHostDao.save(host);
        eventManager.publish(new AgentHostStatusEvent(this, host));
    }

    /**
     * check zk and lock
     */
    private boolean lock() {
        try {
            zk.create(CreateMode.EPHEMERAL, collectTaskZkPath, null);
            return true;
        } catch (ZookeeperException e) {
            log.warn("Unable to init agent host collect task : {}", e.getMessage());
            return false;
        }
    }

    private void clean() {
        try {
            zk.delete(collectTaskZkPath, false);
        } catch (ZookeeperException ignore) {

        }
    }

    //====================================================================
    //        %% Private classes
    //====================================================================

    private Secret getSecret(String secret, Secret.Category expected) {
        GetSecretEvent event = new GetSecretEvent(this, secret);
        eventManager.publish(event);

        Secret c = event.getFetched();
        Preconditions.checkArgument(c != null, "Secret not found");
        Preconditions.checkArgument(c.getCategory() == expected, "Invalid secret category");

        return c;
    }

    private interface HostAdaptor {

        void create(AgentHost host);

        DockerManager init(AgentHost host) throws Exception;

        StartOption buildStartOption(AgentHost host, Agent agent);
    }

    private abstract class AbstractHostAdaptor implements HostAdaptor {

        protected void initStartOption(StartOption option, Agent agent) {
            option.setImage(DefaultImage);
            option.setName(getContainerName(agent));

            option.addEnv(SERVER_URL, serverUrl);
            option.addEnv(AGENT_TOKEN, agent.getToken());
            option.addEnv(AGENT_LOG_LEVEL, "DEBUG");
            option.addEnv(AGENT_VOLUMES, System.getenv(AGENT_VOLUMES));
            option.addEnv(AGENT_WORKSPACE, DefaultWorkspace);
        }
    }

    private class K8sHostAdaptor extends AbstractHostAdaptor {

        @Override
        public void create(AgentHost host) {
            K8sAgentHost k8sHost = (K8sAgentHost) host;
            Preconditions.checkArgument(k8sHost.getSecret() != null, "Secret name must be defined");
            agentHostDao.insert(k8sHost);
        }

        @Override
        public DockerManager init(AgentHost host) throws Exception {
            K8sAgentHost k8sHost = (K8sAgentHost) host;
            KubeConfigSecret secret = (KubeConfigSecret) getSecret(k8sHost.getSecret(), KUBE_CONFIG);
            String namespace = k8sHost.getNamespace();

            K8sOption option = new KubeConfigOption(namespace, secret.getContent().getData());
            K8sManager manager = new K8sManager(option);

            // check namespace
            if (!manager.hasNamespace()) {
                throw new Exception(String.format("namespace '%s' not exist", namespace));
            }

            log.debug("namespace {} found", namespace);

            // create endpoints if service deployed outside k8s
            if (!k8sProperties.enabled()) {
                log.debug("create endpoints in k8s");

                for (K8sAgentHost.Endpoint ep : k8sEndpoints) {
                    K8sCreateEndpointOption endpointOption = new K8sCreateEndpointOption(ep.getName(), ep.getIp(), ep.getPort());
                    manager.createEndpoint(endpointOption);
                    log.info("endpoint {} initialized", namespace);
                }
            }

            log.debug("k8s manager initialized");
            return manager;
        }

        @Override
        public StartOption buildStartOption(AgentHost host, Agent agent) {
            PodStartOption option = new PodStartOption();
            initStartOption(option, agent);

            option.setLabel(ContainerNamePrefix);

            option.addEnv(SERVER_URL, k8sHosts.getServerUrl());
            option.addEnv(AGENT_K8S_ENABLED, Boolean.TRUE.toString());
            option.addEnv(AGENT_K8S_IN_CLUSTER, Boolean.TRUE.toString());

            // TODO: check is deployed in the k8s cluster
            return option;
        }
    }

    private class LocalSocketHostAdaptor extends AbstractHostAdaptor {

        @Override
        public void create(AgentHost host) {
            if (hasCreated()) {
                throw new NotAvailableException("Local unix socket agent host been created");
            }

            if (!Files.exists(Paths.get("/var/run/docker.sock"))) {
                deleteIfExist();
                throw new NotAvailableException("Docker socket not available");
            }

            try {
                host.setCreatedBy(User.DefaultSystemUser);
                agentHostDao.insert(host);
            } catch (Exception e) {
                log.warn("Unable to create local unix socket agent host: {}", e.getMessage());
                throw new NotAvailableException(e.getMessage());
            }
        }

        @Override
        public DockerManager init(AgentHost host) {
            return dockerManager;
        }

        @Override
        public StartOption buildStartOption(AgentHost host, Agent agent) {
            ContainerStartOption option = new ContainerStartOption();
            initStartOption(option, agent);
            option.addBind(DockerSock, DockerSock);
            return option;
        }

        private boolean hasCreated() {
            return agentHostDao.findAllByType(AgentHost.Type.LocalUnixSocket).size() > 0;
        }

        private void deleteIfExist() {
            List<AgentHost> hosts = agentHostDao.findAllByType(AgentHost.Type.LocalUnixSocket);
            if (hosts.isEmpty()) {
                return;
            }
            agentHostDao.deleteAll(hosts);
        }
    }

    private class SshHostAdaptor extends AbstractHostAdaptor {

        @Override
        public void create(AgentHost host) {
            SshAgentHost sshHost = (SshAgentHost) host;
            Preconditions.checkArgument(sshHost.getSecret() != null, "Secret name must be defined");
            agentHostDao.insert(sshHost);
        }

        @Override
        public DockerManager init(AgentHost host) throws Exception {
            SshAgentHost sshHost = (SshAgentHost) host;
            RSASecret rsa = (RSASecret) getSecret(sshHost.getSecret(), SSH_RSA);

            SSHOption option = SSHOption.of(rsa.getPrivateKey(), sshHost.getIp(), sshHost.getUser(), sshHost.getPort());
            return new DockerSSHManager(option);
        }

        @Override
        public StartOption buildStartOption(AgentHost host, Agent agent) {
            ContainerStartOption option = new ContainerStartOption();
            initStartOption(option, agent);
            option.addBind(DockerSock, DockerSock);
            return option;
        }
    }

    @AllArgsConstructor(staticName = "of")
    public static class AgentItemWrapper {

        public static <T> Set<AgentItemWrapper> toSet(List<T> list) {
            Set<AgentItemWrapper> set = new HashSet<>(list.size());
            Iterator<T> iterator = list.iterator();
            for (; iterator.hasNext(); ) {
                set.add(AgentItemWrapper.of(iterator.next()));
                iterator.remove();
            }
            return set;
        }

        private final Object object;

        public String getName() {
            if (object instanceof Agent) {
                return ((Agent) object).getName();
            }

            if (object instanceof Unit) {
                return ((Unit) object).getName();
            }

            throw new IllegalArgumentException();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof AgentItemWrapper) {
                AgentItemWrapper obj = (AgentItemWrapper) o;
                return this.getName().equals(obj.getName());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.getName().hashCode();
        }
    }

    @Log4j2
    private static class PoolManagerRemover implements RemovalListener<AgentHost, DockerManager> {

        @Override
        public void onRemoval(@Nullable AgentHost agentHost,
                              @Nullable DockerManager dockerManager,
                              @Nonnull RemovalCause removalCause) {
            if (dockerManager != null) {
                try {
                    dockerManager.close();
                } catch (Exception e) {
                    log.warn("Unable to close agent host", e);
                }
            }

            if (agentHost != null) {
                log.info("Agent pool manager for host {} been closed", agentHost.getName());
            }
        }
    }
}