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

import com.flow.platform.api.dao.YmlStorageDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.YmlStorage;
import com.flow.platform.api.domain.envs.FlowEnvs;
import com.flow.platform.api.domain.envs.FlowEnvs.YmlStatusValue;
import com.flow.platform.api.domain.envs.GitEnvs;
import com.flow.platform.api.exception.YmlException;
import com.flow.platform.api.service.GitService;
import com.flow.platform.api.task.CloneAndVerifyYmlTask;
import com.flow.platform.api.util.EnvUtil;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.core.util.ThreadUtil;
import com.flow.platform.util.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import java.util.Objects;
import java.util.Set;
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
public class YmlServiceImpl implements YmlService {

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
    private YmlStorageDao ymlStorageDao;

    @Override
    public Node verifyYml(final String path, final String yml) {
        final String rootPath = PathUtil.rootPath(path);
        final Flow flow = nodeService.findFlow(rootPath);

        Node rootFromYml = NodeUtil.buildFromYml(yml);
        if (flow.equals(rootFromYml)) {
            return rootFromYml;
        }

        throw new YmlException("Flow name in yml not match the path");
    }

    @Override
    public String getYmlContent(final String path) {
        final String rootPath = PathUtil.rootPath(path);
        final Flow flow = nodeService.findFlow(rootPath);

        // check FLOW_YML_STATUS
        String ymlStatus = flow.getEnv(FlowEnvs.FLOW_YML_STATUS);

        // for LOADING status if FLOW_YML_STATUS start with GIT_xxx
        if (YmlStatusValue.isLoadingStatus(ymlStatus)) {
            return "";
        }

        // for FOUND status
        if (Objects.equals(ymlStatus, FlowEnvs.YmlStatusValue.FOUND.value())) {

            // load from database
            YmlStorage ymlStorage = ymlStorageDao.get(rootPath);
            if (ymlStorage != null) {
                return ymlStorage.getFile();
            }
        }

        // for NOT_FOUND status
        if (Objects.equals(ymlStatus, FlowEnvs.YmlStatusValue.NOT_FOUND.value())) {
            throw new NotFoundException("Yml content not found");
        }

        // for ERROR status
        if (Objects.equals(ymlStatus, FlowEnvs.YmlStatusValue.ERROR.value())) {
            throw new YmlException("Illegal yml format");
        }

        // yml has not been load
        throw new IllegalStateException("Illegal FLOW_YML_STATUS value");
    }

    @Override
    public Node loadYmlContent(final String path, final Consumer<YmlStorage> callback) {
        final String rootPath = PathUtil.rootPath(path);
        final Flow flow = nodeService.findFlow(rootPath);
        final Set<String> requiredEnvSet = Sets.newHashSet(GitEnvs.FLOW_GIT_URL.name(), GitEnvs.FLOW_GIT_SOURCE.name());

        if (!EnvUtil.hasRequired(flow, requiredEnvSet)) {
            throw new IllegalParameterException("Missing required envs: FLOW_GIT_URL FLOW_GIT_SOURCE");
        }

        if (YmlStatusValue.isLoadingStatus(flow.getEnv(FlowEnvs.FLOW_YML_STATUS))) {
            throw new IllegalStatusException("Yml file is loading");
        }

        // update FLOW_YML_STATUS to LOADING
        nodeService.updateYmlState(flow, YmlStatusValue.GIT_CONNECTING, null);

        try {
            ThreadPoolTaskExecutor executor = findThreadPoolFromCache(flow.getPath());

            // async to load yml file
            executor.execute(new CloneAndVerifyYmlTask(flow, nodeService, gitService, callback));
        } catch (ExecutionException e) {
            LOGGER.warn("Fail to get task executor for node: " + flow.getPath());
            nodeService.updateYmlState(flow, YmlStatusValue.ERROR, e.getMessage());
        }

        return flow;
    }

    @Override
    public void stopLoadYmlContent(String path) {
        final String rootPath = PathUtil.rootPath(path);
        final Flow flow = nodeService.findFlow(rootPath);

        ThreadPoolTaskExecutor executor = nodeThreadPool.getIfPresent(flow.getPath());
        if (executor == null || executor.getActiveCount() == 0) {
            return;
        }

        executor.shutdown();
        nodeThreadPool.invalidate(flow.getPath());

        LOGGER.trace("Yml loading task been stopped for path %s", path);
        nodeService.updateYmlState(flow, YmlStatusValue.NOT_FOUND, null);
    }

    private ThreadPoolTaskExecutor findThreadPoolFromCache(String path) throws ExecutionException {
        return nodeThreadPool.get(path, () -> {
            ThreadPoolTaskExecutor taskExecutor = ThreadUtil
                .createTaskExecutor(NODE_THREAD_POOL_SIZE, NODE_THREAD_POOL_SIZE, 0, "git-clone-task");
            taskExecutor.initialize();
            return taskExecutor;
        });
    }
}
