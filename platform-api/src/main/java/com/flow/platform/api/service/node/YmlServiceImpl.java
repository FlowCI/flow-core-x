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

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.dao.YmlDao;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.Yml;
import com.flow.platform.api.domain.request.ThreadConfigParam;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.envs.FlowEnvs;
import com.flow.platform.api.envs.FlowEnvs.YmlStatusValue;
import com.flow.platform.api.service.GitService;
import com.flow.platform.api.task.UpdateNodeYmlTask;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.core.context.ContextEvent;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.core.util.ThreadUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service
@Log4j2
public class YmlServiceImpl implements YmlService, ContextEvent {

    private final static int NODE_THREAD_POOL_CACHE_EXPIRE = 3600 * 24;

    private final static int NODE_THREAD_POOL_CACHE_SIZE = 100;

    // To cache thread pool for node path
    private Cache<String, ThreadPoolTaskExecutor> nodeThreadPool = CacheBuilder
        .newBuilder()
        .expireAfterAccess(NODE_THREAD_POOL_CACHE_EXPIRE, TimeUnit.SECONDS)
        .maximumSize(NODE_THREAD_POOL_CACHE_SIZE)
        .build();

    private ThreadConfigParam threadConfigParam = new ThreadConfigParam(1, 1, 0, "git-fetch-task");

    @Autowired
    private GitService gitService;

    @Autowired
    private NodeService nodeService;

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
    public Node build(final Node root, final String yml) {
        return NodeUtil.buildFromYml(yml, root.getName());
    }

    @Override
    public String parse(final Node root) {
        return NodeUtil.parseToYml(root);
    }

    @Override
    public void saveOrUpdate(final Node root, final String yml) {
        Yml exist = get(root);
        if (Objects.isNull(exist)) {
            exist = new Yml(root.getPath());
        }

        exist.setFile(yml);
        ymlDao.saveOrUpdate(exist);
    }

    @Override
    public Yml get(final Node root) {
        return get(root.getPath());
    }

    @Override
    public Yml get(String path) {
        return ymlDao.get(path);
    }

    @Override
    public Resource getResource(Node root) {
        Yml yml = ymlDao.get(root.getPath());
        String body = yml.getFile();
        Resource allResource;
        try (InputStream is = new ByteArrayInputStream(body.getBytes(AppConfig.DEFAULT_CHARSET))) {
            allResource = new InputStreamResource(is);
        } catch (Throwable throwable) {
            throw new NotFoundException("yml not found");
        }

        return allResource;
    }

    @Override
    public void delete(Node root) {
        ymlDao.delete(new Yml(root.getPath(), null));
    }

    @Override
    public Node startLoad(final Node root, final Consumer<Yml> onSuccess, final Consumer<Throwable> onError) {
        if (!EnvUtil.hasRequiredEnvKey(root, GitService.REQUIRED_ENVS)) {
            throw new IllegalParameterException("Missing git settings: FLOW_GIT_URL and FLOW_GIT_SOURCE");
        }

        if (isYmlLoading(root)) {
            throw new IllegalStatusException("Yml file is loading");
        }

        // update FLOW_YML_STATUS to LOADING
        nodeService.updateYmlState(root, YmlStatusValue.GIT_CONNECTING, null);

        try {
            ThreadPoolTaskExecutor executor = findThreadPoolFromCache(root.getPath());

            // async to load yml file
            executor.execute(new UpdateNodeYmlTask(root, nodeService, gitService, onSuccess, onError));
        } catch (ExecutionException | TaskRejectedException e) {
            log.warn("Fail to get task executor for node: " + root.getPath());
            nodeService.updateYmlState(root, YmlStatusValue.ERROR, e.getMessage());

            if (onError != null) {
                onError.accept(e);
            }
        }

        return root;
    }

    @Override
    public void stopLoad(final Node root) {
        ThreadPoolTaskExecutor executor = nodeThreadPool.getIfPresent(root.getPath());
        if (executor == null || executor.getActiveCount() == 0) {
            return;
        }

        if (!isYmlLoading(root)) {
            return;
        }

        executor.shutdown();
        nodeThreadPool.invalidate(root.getPath());

        log.trace("Yml loading task been stopped for path {}", root.getPath());
        nodeService.updateYmlState(root, YmlStatusValue.NOT_FOUND, null);
    }

    @Override
    public void threadConfig(ThreadConfigParam threadConfigParam) {
        this.threadConfigParam = threadConfigParam;
        nodeThreadPool.invalidateAll();
    }

    private boolean isYmlLoading(final Node node) {
        String ymlStatus = node.getEnv(FlowEnvs.FLOW_YML_STATUS);
        return YmlStatusValue.isLoadingStatus(ymlStatus);
    }

    private ThreadPoolTaskExecutor findThreadPoolFromCache(String path) throws ExecutionException {
        return nodeThreadPool.get(path, () -> {
            ThreadPoolTaskExecutor taskExecutor = ThreadUtil
                .createTaskExecutor(threadConfigParam.getMaxPoolSize(), threadConfigParam.getCorePoolSize(),
                    threadConfigParam.getQueueSize(), threadConfigParam.getThreadNamePrefix());
            taskExecutor.initialize();
            return taskExecutor;
        });
    }
}
