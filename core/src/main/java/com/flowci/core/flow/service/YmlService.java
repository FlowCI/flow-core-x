/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.flow.service;

import com.flowci.common.exception.NotFoundException;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.SimpleYml;
import com.flowci.core.flow.domain.FlowYml;
import com.flowci.tree.NodeTree;

import java.util.List;

/**
 * @author yang
 */
public interface YmlService {

    /**
     * Get yml list by flow id
     *
     * @throws NotFoundException if YML not found
     */
    FlowYml get(String flowId);

    /**
     * Get NodeTree from yaml
     *
     * @throws NotFoundException if YML not found
     */
    NodeTree getTree(String flowId);

    /**
     * Create or update yml for flow
     */
    FlowYml saveYml(Flow flow, List<SimpleYml> list);

    /**
     * Delete all yaml of flow
     */
    void delete(String flowId);
}
