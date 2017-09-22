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

import com.flow.platform.api.dao.FlowDao;
import com.flow.platform.api.dao.YmlDao;
import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.Webhook;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.domain.node.Yml;
import com.flow.platform.api.domain.envs.FlowEnvs;
import com.flow.platform.api.domain.envs.FlowEnvs.StatusValue;
import com.flow.platform.api.domain.envs.FlowEnvs.YmlStatusValue;
import com.flow.platform.api.domain.envs.GitEnvs;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.exception.YmlException;
import com.flow.platform.api.service.user.UserFlowService;
import com.flow.platform.api.util.EnvUtil;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.util.Logger;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

/**
 * @author yh@firim
 */
@Service(value = "nodeService")
public class NodeServiceImpl implements NodeService {

    private final Logger LOGGER = new Logger(NodeService.class);

    private final static int TREE_CACHE_EXPIRE_SECOND = 3600 * 24;

    private final static int MAX_TREE_CACHE_NUM = 100;

    private final static int INIT_NODE_CACHE_NUM = 10;

    // To cache key is root node (flow) path, value is flatted tree as map
    private Cache<String, NodeTree> treeCache = CacheBuilder
        .newBuilder()
        .expireAfterAccess(TREE_CACHE_EXPIRE_SECOND, TimeUnit.SECONDS)
        .maximumSize(MAX_TREE_CACHE_NUM)
        .build();

    @Autowired
    private YmlService ymlService;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private YmlDao ymlDao;

    @Autowired
    private Path workspace;

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserFlowService userFlowService;

    @Value(value = "${domain}")
    private String domain;

    @Override
    public Node createOrUpdate(final String path, final String yml) {
        final Flow flow = findFlow(PathUtil.rootPath(path));

        if (Strings.isNullOrEmpty(yml)) {
            updateYmlState(flow, FlowEnvs.YmlStatusValue.NOT_FOUND, null);
            return flow;
        }

        if (!Objects.equals(flow.getEnv(FlowEnvs.FLOW_STATUS), StatusValue.READY.value())) {
            throw new IllegalStatusException("Node status not correct, should be READY for FLOW_STATUS");
        }

        Node rootFromYml;
        try {
            rootFromYml = ymlService.verifyYml(path, yml);
        } catch (IllegalParameterException | YmlException e) {
            updateYmlState(flow, FlowEnvs.YmlStatusValue.ERROR, e.getMessage());
            return flow;
        }

        flow.putEnv(FlowEnvs.FLOW_YML_STATUS, FlowEnvs.YmlStatusValue.FOUND);

        // persistent flow type node to flow table with env which from yml
        EnvUtil.merge(rootFromYml, flow, true);
        flowDao.update(flow);

        Yml ymlStorage = new Yml(flow.getPath(), yml);
        ymlDao.saveOrUpdate(ymlStorage);

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
            NodeTree tree = treeCache.get(rootPath, () -> {

                Yml ymlStorage = ymlDao.get(rootPath);
                Flow flow = flowDao.get(path);

                // has related yml
                if (ymlStorage != null) {
                    NodeTree newTree = new NodeTree(ymlStorage.getFile());
                    Node root = newTree.root();

                    // should merge env from flow dao and yml
                    EnvUtil.merge(flow, root, false);

                    return newTree;
                }

                if (flow != null) {
                    return new NodeTree(flow);
                }

                // root path not exist
                return null;
            });

            return tree.find(path);
        } catch (ExecutionException | InvalidCacheLoadException ignore) {
            // not not found or unable to load from cache
            return null;
        }
    }

    /**
     * Find flow by path
     *
     * @param path flow path
     * @return Flow object
     * @throws IllegalParameterException if node path not exist or path is not for flow
     */
    @Override
    public Flow findFlow(String path) {
        Node node = find(path);
        if (node == null) {
            throw new IllegalParameterException("The flow path doesn't exist");
        }

        if (!(node instanceof Flow)) {
            throw new IllegalParameterException("The path is not for flow");
        }

        return (Flow) node;
    }

    @Override
    public Node delete(String path) {
        String rootPath = PathUtil.rootPath(path);
        Flow flow = findFlow(rootPath);

        // delete flow
        flowDao.delete(flow);

        // delete related yml storage
        ymlDao.delete(new Yml(flow.getPath(), null));

        // delete local flow folder
        Path flowWorkspace = NodeUtil.workspacePath(workspace, flow);
        FileSystemUtils.deleteRecursively(flowWorkspace.toFile());

        treeCache.invalidate(rootPath);
        return flow;
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
        flow.putEnv(FlowEnvs.FLOW_STATUS, StatusValue.PENDING);
        flow.putEnv(FlowEnvs.FLOW_YML_STATUS, YmlStatusValue.NOT_FOUND);
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
    public void updateYmlState(Flow flow, FlowEnvs.YmlStatusValue state, String errorInfo) {
        flow.putEnv(FlowEnvs.FLOW_YML_STATUS, state);

        if (!Strings.isNullOrEmpty(errorInfo)) {
            flow.putEnv(FlowEnvs.FLOW_YML_ERROR_MSG, errorInfo);
        } else {
            flow.removeEnv(FlowEnvs.FLOW_YML_ERROR_MSG);
        }

        LOGGER.debug("Update '%s' yml status to %s", flow.getName(), state);
        flowDao.update(flow);
    }

    @Override
    public List<Flow> listFlows() {
        return flowDao.list();
    }

    @Override
    public List<String> listFlowPathByUser(Collection<String> createdByList) {
        return flowDao.pathList(createdByList);
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

    @Override
    public List<User> authUsers(List<String> emailList, String flowPath) {
        List<User> users = userDao.list(emailList);
        List<String> paths = new ArrayList<>();
        paths.add(flowPath);
        Flow flow = findFlow(flowPath);
        for (User user : users) {
            if (user != null) {
                userFlowService.unAssign(user, flow);
                userFlowService.assign(user, flow);
                user.setFlows(paths);
            }
        }
        return users;
    }


    private String hooksUrl(final Flow flow) {
        return String.format("%s/hooks/git/%s", domain, flow.getName());
    }
}
