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

import com.flow.platform.api.dao.FlowDao;
import com.flow.platform.api.dao.YmlStorageDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.Webhook;
import com.flow.platform.api.domain.YmlStorage;
import com.flow.platform.api.exception.NotFoundException;
import com.flow.platform.api.util.EnvUtil;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.exception.IllegalParameterException;
import com.flow.platform.util.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */

@Service(value = "nodeService")
public class NodeServiceImpl implements NodeService {

    private final static int MAX_TREE_CACHE_NUM = 100;

    private final static int INIT_NODE_CACHE_NUM = 10;

    private final Logger LOGGER = new Logger(NodeService.class);

    // To cache key is root node (flow) path, value is flatted tree as map
    private Cache<String, Cache<String, Node>> treeCache =
        CacheBuilder.newBuilder().maximumSize(MAX_TREE_CACHE_NUM).build();

    @Autowired
    private YmlStorageDao ymlStorageDao;

    @Autowired
    private FlowDao flowDao;

    @Value(value = "${domain}")
    private String domain;

    @Override
    public Node create(final String yml) {
        Node root = NodeUtil.buildFromYml(yml);

        // root node must be created
        Flow flow = flowDao.get(root.getPath());
        if (flow == null) {
            throw new NotFoundException("Flow name defined in yml has not been created");
        }

        // persistent flow type node to flow table with env which from yml
        EnvUtil.merge(root, flow, true);
        flowDao.update(flow);

        // TODO: should check md5 of yml to reduce db io
        YmlStorage ymlStorage = ymlStorageDao.get(root.getPath());
        if (ymlStorage == null) {
            ymlStorageDao.save(new YmlStorage(root.getPath(), yml));
        } else {
            ymlStorage.setFile(yml);
            ymlStorageDao.update(ymlStorage);
        }

        // reset cache
        treeCache.invalidate(root.getPath());
        return root;
    }

    @Override
    public Node find(final String path) {
        final String rootPath = PathUtil.rootPath(path);

        try {
            // load tree from tree cache
            Cache<String, Node> tree = treeCache.get(rootPath, () -> {

                YmlStorage ymlStorage = ymlStorageDao.get(rootPath);
                Cache<String, Node> cache = CacheBuilder.newBuilder().initialCapacity(INIT_NODE_CACHE_NUM).build();

                // has related yml
                if (ymlStorage != null) {
                    Node root = NodeUtil.buildFromYml(ymlStorage.getFile());
                    NodeUtil.recurse(root, node -> cache.put(node.getPath(), node));
                    return cache;
                }

                // for empty flow
                Flow flow = flowDao.get(path);
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
    public String rawYml(String path) {
        final String rootPath = PathUtil.rootPath(path);
        YmlStorage ymlStorage = ymlStorageDao.get(rootPath);
        if (ymlStorage == null) {
            return null;
        }
        return ymlStorage.getFile();
    }

    @Override
    public boolean exist(final String path) {
        return find(path) != null;
    }

    @Override
    public Flow createEmptyFlow(final String flowName) {
        Flow flow = new Flow(PathUtil.build(flowName), flowName);

        if (exist(flow.getPath())) {
            throw new IllegalParameterException("Flow name already existed");
        }

        flow = flowDao.save(flow);

        // reset cache
        treeCache.invalidate(flow.getPath());
        return flow;
    }

    @Override
    public void setFlowEnv(String path, Map<String, String> envs) {
        Node node = find(path);
        if (node == null) {
            throw new IllegalParameterException("The flow path doesn't exist");
        }

        if (!(node instanceof Flow)) {
            throw new IllegalParameterException("The path is not for flow");
        }

        node.setEnvs(envs);

        // sync latest env into flow table
        flowDao.update((Flow) node);
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
}
