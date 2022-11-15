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

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.tree.NodeTree;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author yang
 */
public interface YmlService {

    /**
     * List Yml instance without raw field
     */
    List<Yml> list(String flowId);

    /**
     * Get NodeTree from yaml
     *
     * @throws com.flowci.exception.NotFoundException if YML not found
     */
    NodeTree getTree(String flowId, String name);

    /**
     * Get yml by flow id and yaml name
     *
     * @throws com.flowci.exception.NotFoundException if YML not found
     */
    Yml getYml(String flowId, String name);

    /**
     * Get b64 yml string only by flow id and yaml name
     */
    @Nullable
    String getYmlString(String flowId, String name);

    /**
     * Create or update yml for flow
     *
     * @param yml could be null or empty string
     * @throws com.flowci.exception.ArgumentException if yml string is empty or null
     */
    Yml saveYml(Flow flow, String name, String yml);

    Yml saveYmlFromB64(Flow flow, String name, String ymlInB64);

    /**
     * Delete all yaml of flow
     */
    void delete(String flowId);

    /**
     * Delete a yaml in flow
     */
    void delete(String flowId, String name);
}
