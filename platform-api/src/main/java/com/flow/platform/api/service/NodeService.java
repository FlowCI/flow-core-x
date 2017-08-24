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

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.Webhook;
import com.flow.platform.api.domain.YmlStorage;
import com.flow.platform.api.domain.envs.FlowEnvs;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
     * Delete root node
     *
     * @param path any path, will find root path
     */
    Node delete(String path);

    /**
     * Verify yml format
     *
     * @param path any path
     * @param yml yml content
     * @return Node from yml
     */
    Node verifyYml(String path, String yml);

    /**
     * Find raw yml file content by node path from
     * - yam storage
     * - flow workspace if yml storage not found
     *
     * @param path any node path
     * @return <p> - yml content - empty string while loading </p>
     * @throws com.flow.platform.core.exception.NotFoundException if FLOW_YML_STATUS is NOT_FOUND
     * @throws com.flow.platform.api.exception.YmlException if FLOW_YML_STATUS is ERROR
     * @throws IllegalStateException if FLOW_YML_STATUS is illegal
     */
    String getYmlContent(String path);

    /**
     * Load yml content from git repo in async and create tree from yml,
     * Then call "getYmlContent" to get yml
     *
     * @param path any node path
     * @param callback method on yml loaded
     */
    void loadYmlContent(String path, Consumer<YmlStorage> callback);

    /**
     * To update FLOW_YML_STATUS and FLOW_YML_ERROR_MSG
     */
    void updateYmlState(Flow flow, FlowEnvs.YmlStatusValue state, String errorInfo);

    /**
     * To check flow name is existed
     */
    boolean exist(String path);

    /**
     * Create flow without any children
     *
     * - FLOW_STATUS will be set to PENDING
     * - FLOW_GIT_WEBHOOK will be created in env
     */
    Flow createEmptyFlow(String flowName);

    /**
     * Merge new env to flow node evn and sync to yml
     */
    Flow setFlowEnv(String path, Map<String, String> envs);

    /**
     * list current flows with path, name, created at and updated at
     */
    List<Flow> listFlows();

    /**
     * List webhooks for all flow
     */
    List<Webhook> listWebhooks();
}
