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

import com.flow.platform.api.util.PathUtil;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.HandlerMapping;

/**
 * @author yang
 */
@ControllerAdvice(basePackageClasses = {NodeController.class})
public class NodeControllerAdvice {

    private final static String PATH_VAR_ROOT = "root";

    private final static String PATH_VAR_CHILD = "child";

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private ThreadLocal<String> currentNodePath;

    @ModelAttribute
    public void setCurrentNodePath() {
        Map<String, String> attributes =
            (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        String root = attributes.get(PATH_VAR_ROOT);
        String child = attributes.get(PATH_VAR_CHILD);

        String path = PathUtil.build(root, child);
        currentNodePath.set(path);
    }
}
