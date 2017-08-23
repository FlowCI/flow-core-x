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
import com.flow.platform.api.dao.FlowDao;
import com.flow.platform.api.dao.YmlStorageDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.Webhook;
import com.flow.platform.api.domain.YmlStorage;
import com.flow.platform.api.domain.envs.FlowEnvs;
import com.flow.platform.api.domain.envs.GitEnvs;
import com.flow.platform.api.exception.YmlException;
import com.flow.platform.api.util.EnvUtil;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.util.Logger;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */

@Service(value = "nodeService")
public class NodeServiceImpl implements NodeService {

    private final Logger LOGGER = new Logger(NodeService.class);

    private final static int MAX_TREE_CACHE_NUM = 100;

    private final static int INIT_NODE_CACHE_NUM = 10;

    // To cache key is root node (flow) path, value is flatted tree as map
    private Cache<String, Cache<String, Node>> treeCache = CacheBuilder
        .newBuilder()
        .expireAfterAccess(3600 * 24, TimeUnit.SECONDS)
        .maximumSize(MAX_TREE_CACHE_NUM)
        .build();

    @Autowired
    private YmlStorageDao ymlStorageDao;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private GitService gitService;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Value(value = "${domain}")
    private String domain;

    @Override
    public Node createOrUpdate(final String path, final String yml) {
        final Flow flow = findFlow(PathUtil.rootPath(path));
        if (Strings.isNullOrEmpty(yml)) {
            return flow;
        }

        Node rootFromYml;
        try {
            rootFromYml = verifyYml(path, yml);
        } catch (IllegalParameterException | YmlException e) {
            flow.putEnv(FlowEnvs.FLOW_YML_STATUS, FlowEnvs.Value.FLOW_YML_STATUS_ERROR);
            flowDao.update(flow);
            return flow;
        }

        flow.putEnv(FlowEnvs.FLOW_STATUS, FlowEnvs.Value.FLOW_STATUS_READY);
        flow.putEnv(FlowEnvs.FLOW_YML_STATUS, FlowEnvs.Value.FLOW_YML_STATUS_FOUND);

        // persistent flow type node to flow table with env which from yml
        EnvUtil.merge(rootFromYml, flow, true);
        flowDao.update(flow);

        YmlStorage ymlStorage = new YmlStorage(flow.getPath(), yml);
        ymlStorageDao.saveOrUpdate(ymlStorage);

        // reset cache
        treeCache.invalidate(flow.getPath());

        //retry find flow
        return findFlow(PathUtil.rootPath(path));
    }

    @Override
    public Node find(final String path) {
        final String rootPath = PathUtil.rootPath(path);

        try {
            // load tree from tree cache
            Cache<String, Node> tree = treeCache.get(rootPath, () -> {

                YmlStorage ymlStorage = ymlStorageDao.get(rootPath);
                Flow flow = flowDao.get(path);

                Cache<String, Node> cache = CacheBuilder.newBuilder().initialCapacity(INIT_NODE_CACHE_NUM).build();

                // has related yml
                if (ymlStorage != null) {
                    Node root = NodeUtil.buildFromYml(ymlStorage.getFile());
                    NodeUtil.recurse(root, node -> cache.put(node.getPath(), node));

                    // should merge env from flow dao and yml
                    EnvUtil.merge(flow, root, false);

                    // should set created time and updated time
                    if (flow != null) {
                        root.setCreatedAt(flow.getCreatedAt());
                        root.setUpdatedAt(flow.getUpdatedAt());
                    }

                    return cache;
                }

                if (flow != null) {
                    cache.put(flow.getPath(), flow);
                    return cache;
                }

                // root path not exist
                return null;
            });

            return tree.getIfPresent(path);
        } catch (ExecutionException | InvalidCacheLoadException ignore) {
            // not not found or unable to load from cache
            return null;
        }
    }

    @Override
    public Node delete(String path) {
        String rootPath = PathUtil.rootPath(path);
        Flow flow = findFlow(rootPath);

        flowDao.delete(flow);
        ymlStorageDao.delete(new YmlStorage(flow.getPath(), null));

        treeCache.invalidate(rootPath);
        return flow;
    }

    @Override
    public Node verifyYml(final String path, final String yml) {
        final String rootPath = PathUtil.rootPath(path);
        final Flow flow = findFlow(rootPath);

        Node rootFromYml = NodeUtil.buildFromYml(yml);
        if (flow.equals(rootFromYml)) {
            return rootFromYml;
        }

        throw new YmlException("Flow name in yml not match the path");
    }

    @Override
    public String getYmlContent(final String path) {
        final String rootPath = PathUtil.rootPath(path);
        final Flow flow = findFlow(rootPath);

        // check FLOW_YML_STATUS
        String ymlStatus = flow.getEnv(FlowEnvs.FLOW_YML_STATUS);

        // for LOADING status
        if (Objects.equals(ymlStatus, FlowEnvs.Value.FLOW_YML_STATUS_LOADING.value())) {
            return "";
        }

        // for FOUND status
        if (Objects.equals(ymlStatus, FlowEnvs.Value.FLOW_YML_STATUS_FOUND.value())) {

            // load from database
            YmlStorage ymlStorage = ymlStorageDao.get(rootPath);
            if (ymlStorage != null) {
                return ymlStorage.getFile();
            }
        }

        // for NOT_FOUND status
        if (Objects.equals(ymlStatus, FlowEnvs.Value.FLOW_YML_STATUS_NOT_FOUND.value())) {
            throw new NotFoundException("Yml content not found");
        }

        // for ERROR status
        if (Objects.equals(ymlStatus, FlowEnvs.Value.FLOW_YML_STATUS_ERROR.value())) {
            throw new YmlException("Illegal yml format");
        }

        // yml has not been load
        throw new IllegalStateException("Illegal FLOW_YML_STATUS value");
    }

    @Override
    public void loadYmlContent(final String path, final Consumer<YmlStorage> callback) {
        final String rootPath = PathUtil.rootPath(path);
        final Flow flow = findFlow(rootPath);
        final Set<String> requiredEnvSet = Sets.newHashSet(GitEnvs.FLOW_GIT_URL.name(), GitEnvs.FLOW_GIT_SOURCE.name());

        if (!EnvUtil.hasRequired(flow, requiredEnvSet)) {
            throw new IllegalParameterException("Missing required envs");
        }

        if (Objects.equals(flow.getEnv(FlowEnvs.FLOW_YML_STATUS), FlowEnvs.Value.FLOW_YML_STATUS_LOADING.value())) {
            throw new IllegalStatusException("Yml file is loading");
        }

        // update FLOW_YML_STATUS to LOADING
        flow.putEnv(FlowEnvs.FLOW_YML_STATUS, FlowEnvs.Value.FLOW_YML_STATUS_LOADING);
        flowDao.update(flow);

        // async to load yml file
        taskExecutor.execute(() -> {
            String yml = gitService.clone(flow, AppConfig.DEFAULT_YML_FILE);

            try {
                createOrUpdate(path, yml);
            } catch (Throwable e) {
                LOGGER.warn("Fail to create or update yml in node");
            }

            // call consumer
            if (callback != null) {
                callback.accept(new YmlStorage(flow.getPath(), yml));
            }
        });
    }

    @Override
    public boolean exist(final String path) {
        return find(path) != null;
    }

    @Override
    public Flow createEmptyFlow(final String flowName) {
        Flow flow = new Flow(PathUtil.build(flowName), flowName);
        treeCache.invalidate(flow.getPath());

        if (exist(flow.getPath())) {
            throw new IllegalParameterException("Flow name already existed");
        }

        flow.putEnv(GitEnvs.FLOW_GIT_WEBHOOK, hooksUrl(flow));
        flow.putEnv(FlowEnvs.FLOW_STATUS, FlowEnvs.Value.FLOW_STATUS_PENDING);
        flow.putEnv(FlowEnvs.FLOW_YML_STATUS, FlowEnvs.Value.FLOW_YML_STATUS_NOT_FOUND);
        flow = flowDao.save(flow);

        return flow;
    }

    @Override
    public Flow setFlowEnv(String path, Map<String, String> envs) {
        Flow flow = findFlow(path);
        EnvUtil.merge(envs, flow.getEnvs(), true);

        // sync latest env into flow table
        flowDao.update(flow);
        return flow;
    }

    @Override
    public List<Flow> listFlows() {
        return flowDao.list();
    }

    @Override
    public List<Webhook> listWebhooks() {
        List<Flow> flows = listFlows();
        List<Webhook> hooks = new ArrayList<>(flows.size());
        for (Flow flow : flows) {
            hooks.add(new Webhook(flow.getPath(), hooksUrl(flow)));
        }
        return hooks;
    }

    private String hooksUrl(final Flow flow) {
        return String.format("%s/hooks/git/%s", domain, flow.getName());
    }

    /**
     * Find flow by path
     *
     * @param path flow path
     * @return Flow object
     * @throws IllegalParameterException if node path not exist or path is not for flow
     */
    private Flow findFlow(String path) {
        Node node = find(path);
        if (node == null) {
            throw new IllegalParameterException("The flow path doesn't exist");
        }

        if (!(node instanceof Flow)) {
            throw new IllegalParameterException("The path is not for flow");
        }

        return (Flow) node;
    }
}
