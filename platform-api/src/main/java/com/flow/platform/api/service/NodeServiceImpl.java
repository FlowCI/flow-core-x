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
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.exception.IllegalParameterException;
import com.flow.platform.exception.NotImplementedException;
import com.flow.platform.util.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */

@Service(value = "nodeService")
public class NodeServiceImpl implements NodeService {

    private final Logger LOGGER = new Logger(NodeService.class);

    private Cache<String, Node> nodeCache = CacheBuilder.newBuilder().maximumSize(1000).build();

    @Autowired
    private YmlStorageDao ymlStorageDao;

    @Autowired
    private FlowDao flowDao;

    @Value(value = "${domain}")
    private String domain;

    @Override
    public Node create(final String yml) {
        Node root = NodeUtil.buildFromYml(yml);

        // persistent flow type node to flow table
        flowDao.save((Flow) root);

        // TODO: should check md5 of yml to reduce db io
        YmlStorage ymlStorage = ymlStorageDao.get(root.getPath());
        if (ymlStorage == null) {
            ymlStorageDao.save(new YmlStorage(root.getPath(), yml));
        } else {
            ymlStorage.setFile(yml);
            ymlStorageDao.update(ymlStorage);
        }

        // save to cache
        NodeUtil.recurse(root, item -> nodeCache.put(item.getPath(), item));
        return root;
    }

    @Override
    public void setEnv(String path, Map<String, String> envs) {
        throw new NotImplementedException();
        //TODO: convert node tree to yml and save
    }

    @Override
    public Node find(final String path) {
        try {
            return nodeCache.get(path, () -> {
                // find root path to load related yml
                String rootPath = PathUtil.rootPath(path);
                YmlStorage ymlStorage = ymlStorageDao.get(rootPath);

                // the root node does not have related yml
                if (ymlStorage == null) {
                    return null;
                }

                Node root = NodeUtil.buildFromYml(ymlStorage.getFile());
                final List<Node> target = new ArrayList<>(1);
                NodeUtil.recurse(root, node -> {
                    nodeCache.put(node.getPath(), node);
                    if (Objects.equals(node.getPath(), path)) {
                        target.add(node);
                    }
                });

                return target.get(0);
            });
        } catch (ExecutionException ignore) {
            return null;
        }
    }

    @Override
    public boolean isExistedFlow(final String flowName) {
        String path = PathUtil.build(flowName);
        return flowDao.get(path) != null;
    }

    @Override
    public Flow createEmptyFlow(final String flowName) {
        Flow flow = new Flow(PathUtil.build(flowName), flowName);

        if (isExistedFlow(flow.getName())) {
            throw new IllegalParameterException("Flow name already existed");
        }

        flow = flowDao.save(flow);
        nodeCache.put(flow.getPath(), flow);
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
}
