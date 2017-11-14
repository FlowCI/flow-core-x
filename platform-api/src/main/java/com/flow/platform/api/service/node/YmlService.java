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

import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.Yml;
import com.flow.platform.api.domain.request.ThreadConfigParam;
import java.util.function.Consumer;
import org.springframework.core.io.Resource;

/**
 * @author yang
 */
public interface YmlService {

    /**
     * Verify yml format
     *
     * @param root root node
     * @param yml yml content
     * @return Node from yml
     */
    Node verifyYml(Node root, String yml);

    /**
     * Find raw yml file content by node path from
     * - yam storage
     * - flow workspace if yml storage not found
     *
     * @param root root node
     * @return <p> - yml content - empty string while loading </p>
     * @throws com.flow.platform.core.exception.NotFoundException if FLOW_YML_STATUS is NOT_FOUND
     * @throws com.flow.platform.api.exception.YmlException if FLOW_YML_STATUS is ERROR
     * @throws IllegalStateException if FLOW_YML_STATUS is illegal
     */
    Yml get(Node root);

    /**
     * yml content to resource
     * @param root
     * @return
     */
    Resource getResource(Node root);

    /**
     * Load yml content from git repo in async and create tree from yml,
     * Then call "get" to get yml
     *
     * @param root root node
     * @param onSuccess method on yml loaded
     * @param onError method on
     * @return flow node instance
     */
    Node startLoad(Node root, Consumer<Yml> onSuccess, Consumer<Throwable> onError);

    /**
     * Stop yml content loading thread
     *
     * @param root root node
     */
    void stopLoad(Node root);

    /**
     * Config load yml thread pool
     *
     * @param threadConfigParam
     */
    void threadConfig(ThreadConfigParam threadConfigParam);

}
