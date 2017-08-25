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

import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.YmlStorage;
import java.util.function.Consumer;

/**
 * @author yang
 */
public interface YmlService {

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
     * @return flow node instance
     */
    Node loadYmlContent(String path, Consumer<YmlStorage> callback);

    /**
     * Stop yml content loading thread
     *
     * @param path
     */
    void stopLoadYmlContent(String path);

}
