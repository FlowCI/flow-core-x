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

import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.api.util.PathUtil;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.NotFoundException;
import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Base node controller, the path of node path variable used with {root}/{child}
 * using 'getNodePathFromUrl' to get node path instead of @PathVariable
 *
 * @author yang
 */
@RestController
@RequestMapping(path = {
    "/nodes/{root}",
    "/nodes/{root}/{child}",
})
public class NodeController {

    protected final static String PATH_VAR_ROOT = "root";

    protected final static String PATH_VAR_CHILD = "child";

    @Autowired
    protected NodeService nodeService;

    @Autowired
    protected HttpServletRequest request;

    /**
     * @api {get} /nodes/:root/:child/env/:key Get Env
     * @apiParam {String} root root node name
     * @apiParam {String} [child] child node name
     * @apiParam {String} [key] env variable name
     * @apiGroup Nodes
     * @apiDescription Get node env by path or name
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      FLOW_ENV_VAR: xxx
     *  }
     */
    @GetMapping(path = "/env/{key}")
    public Map<String, String> getEnv(@PathVariable(required = false) String key) {
        String path = getNodePathFromUrl();

        // check is path for root name
        if (PathUtil.isRootName(path)) {
            path = PathUtil.build(path);
        }

        Node node = nodeService.find(path);

        if (node == null) {
            throw new IllegalParameterException("Invalid node path");
        }

        if (Strings.isNullOrEmpty(key)) {
            return node.getEnvs();
        }

        String env = node.getEnv(key);
        if (Strings.isNullOrEmpty(env)) {
            throw new NotFoundException("Env key is not exist");
        }

        Map<String, String> singleEnv = new HashMap<>(1);
        singleEnv.put(key, env);
        return singleEnv;
    }

    protected String getNodePathFromUrl() {
        Map<String, String> attributes =
            (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        String root = attributes.get(PATH_VAR_ROOT);
        String child = attributes.get(PATH_VAR_CHILD);

        return PathUtil.build(root, child);
    }
}
