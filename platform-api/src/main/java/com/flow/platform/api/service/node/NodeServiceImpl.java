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
import com.flow.platform.api.domain.Webhook;
import com.flow.platform.api.domain.envs.FlowEnvs;
import com.flow.platform.api.domain.envs.FlowEnvs.StatusValue;
import com.flow.platform.api.domain.envs.FlowEnvs.YmlStatusValue;
import com.flow.platform.api.domain.envs.GitEnvs;
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.domain.node.Yml;
import com.flow.platform.api.domain.request.TriggerParam;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.domain.user.SysRole;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.exception.YmlException;
import com.flow.platform.api.service.CurrentUser;
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.api.service.user.UserFlowService;
import com.flow.platform.api.util.EnvUtil;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.util.Logger;
import com.flow.platform.util.http.HttpURL;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

/**
 * @author yh@firim
 */
@Service(value = "nodeService")
@Transactional
public class NodeServiceImpl extends CurrentUser implements NodeService {

    private final Logger LOGGER = new Logger(NodeService.class);

    @Autowired
    private CacheManager cacheManager;

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

    @Autowired
    private JobService jobService;

    @Autowired
    private RoleService roleService;

    @Value(value = "${domain.api}")
    private String apiDomain;

    @Override
    public Node createOrUpdate(final String path, String yml) {
        final Flow flow = findFlow(PathUtil.rootPath(path));

        if (!checkFlowName(flow.getName())) {
            throw new IllegalParameterException("flowName format not true");
        }

        if (Strings.isNullOrEmpty(yml)) {
            updateYmlState(flow, FlowEnvs.YmlStatusValue.NOT_FOUND, null);
            return flow;
        }

        Node rootFromYml;
        try {
            rootFromYml = ymlService.verifyYml(flow, yml);
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
        getTreeCache().evict(flow.getPath());

        //retry find flow
        return findFlow(PathUtil.rootPath(path));
    }

    @Override
    public Node find(final String path) {
        final String rootPath = PathUtil.rootPath(path);

        // load tree from tree cache
        NodeTree tree = getTreeCache().get(rootPath, () -> {

            Yml ymlStorage = ymlDao.get(rootPath);
            Flow flow = flowDao.get(path);

            // has related yml
            if (ymlStorage != null) {
                NodeTree newTree = new NodeTree(ymlStorage.getFile(), flow.getName());
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

        // cleanup cache for null value
        if (tree == null) {
            getTreeCache().evict(rootPath);
            return null;
        }

        return tree.find(path);
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

        // delete related userAuth
        userFlowService.unAssign(flow);

        // delete job
        jobService.delete(rootPath);

        // delete flow
        flowDao.delete(flow);

        // delete related yml storage
        ymlDao.delete(new Yml(flow.getPath(), null));

        // delete local flow folder
        Path flowWorkspace = NodeUtil.workspacePath(workspace, flow);
        FileSystemUtils.deleteRecursively(flowWorkspace.toFile());
        getTreeCache().evict(rootPath);

        // stop yml loading tasks
        ymlService.stopLoadYmlContent(flow);

        return flow;
    }

    @Override
    public boolean exist(final String path) {
        return find(path) != null;
    }

    @Override
    public Flow createEmptyFlow(final String flowName) {
        Flow flow = new Flow(PathUtil.build(flowName), flowName);
        getTreeCache().evict(flow.getPath());

        if (!checkFlowName(flow.getName())) {
            throw new IllegalParameterException("Illegal flow name");
        }

        if (exist(flow.getPath())) {
            throw new IllegalParameterException("Flow name already existed");
        }

        flow.putEnv(GitEnvs.FLOW_GIT_WEBHOOK, hooksUrl(flow));
        flow.putEnv(FlowEnvs.FLOW_STATUS, StatusValue.PENDING);
        flow.putEnv(FlowEnvs.FLOW_YML_STATUS, YmlStatusValue.NOT_FOUND);
        flow.setCreatedBy(currentUser().getEmail());
        flow = flowDao.save(flow);

        userFlowService.assign(currentUser(), flow);
        return flow;
    }

    @Override
    public Flow addFlowEnv(Flow flow, Map<String, String> envs) {
        EnvUtil.merge(envs, flow.getEnvs(), true);

        // sync latest env into flow table
        flowDao.update(flow);
        return flow;
    }

    @Override
    public Flow delFlowEnv(Flow flow, Set<String> keys) {
        for (String keyToRemove : keys) {
            flow.removeEnv(keyToRemove);
        }

        flowDao.update(flow);
        return flow;
    }

    @Override
    public void updateYmlState(Node root, FlowEnvs.YmlStatusValue state, String errorInfo) {
        root.putEnv(FlowEnvs.FLOW_YML_STATUS, state);

        if (!Strings.isNullOrEmpty(errorInfo)) {
            root.putEnv(FlowEnvs.FLOW_YML_ERROR_MSG, errorInfo);
        } else {
            root.removeEnv(FlowEnvs.FLOW_YML_ERROR_MSG);
        }

        LOGGER.debug("Update '%s' yml status to %s", root.getName(), state);
        flowDao.update((Flow) root);
    }

    @Override
    public List<Flow> listFlows() {
        List<Role> roles = roleService.list(currentUser());
        if (roles.contains(roleService.find(SysRole.ADMIN.name()))) {
            return flowDao.list();
        } else {
            return userFlowService.list(currentUser());
        }


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
    public List<User> authUsers(List<String> emailList, String rootPath) {
        List<User> users = userDao.list(emailList);

        List<String> paths = Lists.newArrayList(rootPath);

        Flow flow = findFlow(rootPath);
        for (User user : users) {
            userFlowService.unAssign(user, flow);
            userFlowService.assign(user, flow);
            user.setRoles(roleService.list(user));
            user.setFlows(paths);
        }
        return users;
    }

    @Override
    public Flow updateTrigger(String rootPath, TriggerParam triggerParam){
        Flow flow = findFlow(rootPath);
        flow.setBranchFilter(triggerParam.getBranchFilter());
        flow.setTagFilter(triggerParam.getTagFilter());
        flow.setPrEnable(triggerParam.isPrEnabled());
        flow.setPushEnable(triggerParam.isPushEnabled());
        flow.setTagEnable(triggerParam.isTagEnabled());
        return flowDao.save(flow);
    }

    private String hooksUrl(final Flow flow) {
        return HttpURL.build(apiDomain).append("/hooks/git/").append(flow.getName()).toString();
    }

    private Boolean checkFlowName(String flowName) {
        if (flowName == null || flowName.trim().equals("")) {
            return false;
        }

        if (!Pattern.compile("^\\w{4,20}$").matcher(flowName).matches()) {
            return false;
        }

        return true;
    }

    private Cache getTreeCache() {
        return cacheManager.getCache("treeCache");
    }
}
