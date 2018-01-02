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

package com.flow.platform.api.controller;

import com.flow.platform.api.service.node.EnvService;
import com.flow.platform.api.service.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base node controller, the path of node path variable used with {root}/{child}
 * using 'getNodePathFromUrl' to get node path instead of @PathVariable
 *
 * @author yang
 */
public abstract class NodeController {

    @Autowired
    protected NodeService nodeService;

    @Autowired
    protected EnvService envService;

    /**
     * The current node path from {@see NodeControllerAdvice}
     */
    @Autowired
    protected ThreadLocal<String> currentNodePath;
}
