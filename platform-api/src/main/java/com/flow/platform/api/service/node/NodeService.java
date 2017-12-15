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

import com.flow.platform.api.domain.Webhook;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.envs.FlowEnvs;
import java.util.List;

/**
 * @author yh@firim
 */
public interface NodeService {

    /**
     * Create or update tree from yml config file content
     * and persistent flow node and yml content
     *
     * It will set two env in the flow
     *
     * - FLOW_STATUS = READY
     * - FLOW_YML_STATUS = FOUND or ERROR
     *
     * @param path any path
     * @param yml raw yml
     * @return root node
     */
    Node updateByYml(String path, String yml);

    /**
     * Create or update tree by yml which convert from children nodes
     *
     * @param path any path
     * @param children
     * @return
     */
    Node updateByNodes(String path, List<Node> children);

    /**
     * Find node by node path from yml
     *
     * @return node from path or null if not found
     */
    NodeTree find(String path);

    /**
     * Delete root node
     *
     * @param path any path, will find root path
     */
    Node delete(String path);

    /**
     * To check flow name is existed
     */
    boolean exist(String path);

    /**
     * Create flow without any children
     *
     * - FLOW_STATUS will be set to PENDING
     * - FLOW_GIT_WEBHOOK will be created in env
     * - FLOW_YML_STATUS will be set to NOT_FOUND
     */
    Node createEmptyFlow(String flowName);

    /**
     * To update FLOW_YML_STATUS and FLOW_YML_ERROR_MSG
     */
    void updateYmlState(Node root, FlowEnvs.YmlStatusValue state, String errorInfo);

    /**
     * list current flows with path, name, created at and updated at
     *
     * @param isOnlyCurrentUser load flow only for current user
     */
    List<Node> listFlows(boolean isOnlyCurrentUser);

    /**
     * List webhooks for all flow
     */
    List<Webhook> listWebhooks();

    List<User> authUsers(List<String> emailList, String rootPath);
}
