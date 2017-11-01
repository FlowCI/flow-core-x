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

import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.Webhook;
import com.flow.platform.api.domain.envs.FlowEnvs;
import com.flow.platform.api.domain.request.TriggerParam;
import com.flow.platform.api.domain.user.User;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    Node createOrUpdate(String path, String yml);

    /**
     * Find node by node path from yml
     *
     * @return node from path or null if not found
     */
    Node find(String path);

    /**
     * Find root flow node by path
     */
    Flow findFlow(String path);

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
    Flow createEmptyFlow(String flowName);

    /**
     * Merge new env to flow node evn and sync to yml
     */
    Flow addFlowEnv(Flow flow, Map<String, String> envs);

    /**
     * Delete flow env
     */
    Flow delFlowEnv(Flow flow, Set<String> keys);

    /**
     * To update FLOW_YML_STATUS and FLOW_YML_ERROR_MSG
     */
    void updateYmlState(Node root, FlowEnvs.YmlStatusValue state, String errorInfo);

    /**
     * list current flows with path, name, created at and updated at
     */
    List<Flow> listFlows();

    /**
     * List flow path by created by user
     */
    List<String> listFlowPathByUser(Collection<String> createdByList);

    /**
     * List webhooks for all flow
     */
    List<Webhook> listWebhooks();

    List<User> authUsers(List<String> emailList, String rootPath);

    Flow updateTrigger(String path, TriggerParam triggerParam);
}
