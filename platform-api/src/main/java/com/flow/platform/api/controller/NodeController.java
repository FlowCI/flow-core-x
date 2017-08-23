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

import com.flow.platform.api.domain.Node;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.core.exception.IllegalParameterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author yang
 */
public abstract class NodeController {

    @Autowired
    protected NodeService nodeService;

    @GetMapping("/env")
    public String getEnv(@RequestParam String path, @RequestParam String key) {
        Node node = nodeService.find(path);
        if (node != null) {
            return node.getEnv(key);
        }

        throw new IllegalParameterException("Invalid node path");
    }
}
