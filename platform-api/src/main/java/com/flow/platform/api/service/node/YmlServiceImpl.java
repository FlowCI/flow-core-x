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

package com.flow.platform.api.service.node;

import com.flow.platform.api.dao.YmlDao;
import com.flow.platform.api.domain.credential.Credential;
import com.flow.platform.api.domain.credential.CredentialType;
import com.flow.platform.api.domain.credential.RSACredentialDetail;
import com.flow.platform.api.domain.credential.UsernameCredentialDetail;
import com.flow.platform.api.domain.envs.FlowEnvs;
import com.flow.platform.api.domain.envs.FlowEnvs.YmlStatusValue;
import com.flow.platform.api.domain.envs.GitEnvs;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.Yml;
import com.flow.platform.api.exception.NodeSettingsException;
import com.flow.platform.api.exception.YmlException;
import com.flow.platform.api.service.CredentialService;
import com.flow.platform.api.service.GitService;
import com.flow.platform.api.task.UpdateNodeYmlTask;
import com.flow.platform.api.util.EnvUtil;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.core.context.ContextEvent;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.core.util.ThreadUtil;
import com.flow.platform.util.Logger;
import com.flow.platform.util.git.model.GitSource;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service
public class YmlServiceImpl implements YmlService, ContextEvent {

    private final static Logger LOGGER = new Logger(YmlService.class);

    private final static int NODE_THREAD_POOL_CACHE_EXPIRE = 3600 * 24;

    private final static int NODE_THREAD_POOL_CACHE_SIZE = 100;

    private final static int NODE_THREAD_POOL_SIZE = 1;

    // To cache thread pool for node path
    private Cache<String, ThreadPoolTaskExecutor> nodeThreadPool = CacheBuilder
        .newBuilder()
        .expireAfterAccess(NODE_THREAD_POOL_CACHE_EXPIRE, TimeUnit.SECONDS)
        .maximumSize(NODE_THREAD_POOL_CACHE_SIZE)
        .build();

    @Autowired
    private GitService gitService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private YmlDao ymlDao;

    @Override
    public void start() {
        // ignore
    }

    /**
     * Stop all thread from node thread pool
     */
    @Override
    public void stop() {
        for (Map.Entry<String, ThreadPoolTaskExecutor> entry : nodeThreadPool.asMap().entrySet()) {
            entry.getValue().shutdown();
        }
        nodeThreadPool.invalidateAll();
    }

    @Override
    public Node verifyYml(final Node root, final String yml) {
        return NodeUtil.buildFromYml(yml, root.getName());
    }

    @Override
    public String getYmlContent(final Node root) {
        // for LOADING status if FLOW_YML_STATUS start with GIT_xxx
        if (isYmlLoading(root)) {
            return "";
        }

        // check FLOW_YML_STATUS
        String ymlStatus = root.getEnv(FlowEnvs.FLOW_YML_STATUS);

        // for FOUND status
        if (Objects.equals(ymlStatus, FlowEnvs.YmlStatusValue.FOUND.value())) {

            // load from database
            Yml ymlStorage = ymlDao.get(root.getPath());
            if (ymlStorage != null) {
                return ymlStorage.getFile();
            }

            throw new IllegalParameterException("The yml cannot find by path: " + root.getPath());
        }

        // for NOT_FOUND status
        if (Objects.equals(ymlStatus, FlowEnvs.YmlStatusValue.NOT_FOUND.value())) {
            throw new NotFoundException("Yml content not found");
        }

        // for ERROR status
        if (Objects.equals(ymlStatus, FlowEnvs.YmlStatusValue.ERROR.value())) {
            final String errorMessage = root.getEnv(FlowEnvs.FLOW_YML_ERROR_MSG);
            throw new YmlException("Yml failure : " + errorMessage);
        }

        // yml has not been load
        throw new IllegalStateException("Illegal FLOW_YML_STATUS value");
    }

    @Override
    public Node loadYmlContent(final Node root, final Consumer<Yml> onSuccess, final Consumer<Throwable> onError) {
        if (!EnvUtil.hasRequiredEnvKey(root, GitService.REQUIRED_ENVS)) {
            throw new IllegalParameterException("Missing git settings: FLOW_GIT_URL and FLOW_GIT_SOURCE");
        }

        if (isYmlLoading(root)) {
            throw new IllegalStatusException("Yml file is loading");
        }

        findNodeCredential(root);

        // update FLOW_YML_STATUS to LOADING
        nodeService.updateYmlState(root, YmlStatusValue.GIT_CONNECTING, null);

        try {
            ThreadPoolTaskExecutor executor = findThreadPoolFromCache(root.getPath());

            // async to load yml file
            executor.execute(new UpdateNodeYmlTask(root, nodeService, gitService, onSuccess, onError));
        } catch (ExecutionException e) {
            LOGGER.warn("Fail to get task executor for node: " + root.getPath());
            nodeService.updateYmlState(root, YmlStatusValue.ERROR, e.getMessage());
        }

        return root;
    }

    @Override
    public void stopLoadYmlContent(final Node root) {
        ThreadPoolTaskExecutor executor = nodeThreadPool.getIfPresent(root.getPath());
        if (executor == null || executor.getActiveCount() == 0) {
            return;
        }

        if (!isYmlLoading(root)) {
            return;
        }

        executor.shutdown();
        nodeThreadPool.invalidate(root.getPath());

        LOGGER.trace("Yml loading task been stopped for path %s", root.getPath());
        nodeService.updateYmlState(root, YmlStatusValue.NOT_FOUND, null);
    }

    private boolean isYmlLoading(final Node node) {
        String ymlStatus = node.getEnv(FlowEnvs.FLOW_YML_STATUS);
        return YmlStatusValue.isLoadingStatus(ymlStatus);
    }

    /**
     * Find FLOW_GIT_CREDENTIAL and load from CredentialService
     */
    private void findNodeCredential(Node node) {
        String rsaOrUsernameCredentialName = node.getEnv(GitEnvs.FLOW_GIT_CREDENTIAL);

        if (Strings.isNullOrEmpty(rsaOrUsernameCredentialName)) {
            return;
        }

        try {
            Credential credential = credentialService.find(rsaOrUsernameCredentialName);
            CredentialType credentialType = credential.getType();

            // for git ssh client needs rsa credential
            if (credentialType.equals(CredentialType.RSA)) {
                if (!node.getEnv(GitEnvs.FLOW_GIT_SOURCE).equals(GitSource.UNDEFINED_SSH.name())) {
                    throw new NodeSettingsException("The SSH git source need RSA credential");
                }

                RSACredentialDetail credentialDetail = (RSACredentialDetail) credential.getDetail();
                node.putEnv(GitEnvs.FLOW_GIT_SSH_PRIVATE_KEY, credentialDetail.getPrivateKey());
                node.putEnv(GitEnvs.FLOW_GIT_SSH_PUBLIC_KEY, credentialDetail.getPublicKey());
                return;
            }

            // for git http client needs username credential
            if (credentialType.equals(CredentialType.USERNAME)) {
                if (!node.getEnv(GitEnvs.FLOW_GIT_SOURCE).equals(GitSource.UNDEFINED_HTTP.name())) {
                    throw new NodeSettingsException("The HTTP git source need USERNAME credential");
                }

                UsernameCredentialDetail credentialDetail = (UsernameCredentialDetail) credential.getDetail();
                node.putEnv(GitEnvs.FLOW_GIT_HTTP_USER, credentialDetail.getUsername());
                node.putEnv(GitEnvs.FLOW_GIT_HTTP_PASS, credentialDetail.getPassword());
                return;
            }

            throw new NodeSettingsException("Unsupported credential settings");

        } catch (IllegalParameterException ignore) {
            // credential not found
        }
    }

    private ThreadPoolTaskExecutor findThreadPoolFromCache(String path) throws ExecutionException {
        return nodeThreadPool.get(path, () -> {
            ThreadPoolTaskExecutor taskExecutor = ThreadUtil
                .createTaskExecutor(NODE_THREAD_POOL_SIZE, NODE_THREAD_POOL_SIZE, 0, "git-fetch-task");
            taskExecutor.initialize();
            return taskExecutor;
        });
    }
}
