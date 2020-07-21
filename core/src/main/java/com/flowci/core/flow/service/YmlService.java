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
import com.flowci.tree.FlowNode;

import javax.annotation.Nullable;

/**
 * @author yang
 */
public interface YmlService {

    /**
     * Get FlowNode that represent yaml in obj
     *
     * @throws com.flowci.exception.NotFoundException if YML not found
     */
    FlowNode getRaw(Flow flow);

    /**
     * Get yml by flow
     *
     * @throws com.flowci.exception.NotFoundException if YML not found
     */
    Yml getYml(Flow flow);

    /**
     * Get yml by flow
     */
    @Nullable
    String getYmlString(Flow flow);

    /**
     * Create or update yml for flow
     *
     * @throws com.flowci.exception.ArgumentException if yml string is empty or null
     */
    Yml saveYml(Flow flow, String yml);

    /**
     * Delete flow yml
     */
    void delete(Flow flow);
}
