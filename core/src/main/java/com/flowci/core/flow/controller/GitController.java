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

package com.flowci.core.flow.controller;

import com.flowci.common.helper.StringHelper;
import com.flowci.core.auth.annotation.Action;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.FlowAction;
import com.flowci.core.flow.domain.FlowGitTest;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.GitConnService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@RestController
@RequestMapping("/flows")
public class GitController {

    @Autowired
    private FlowService flowService;

    @Autowired
    private GitConnService gitConnService;

    @PostMapping(value = "/{name}/git/test")
    @Action(FlowAction.GIT_TEST)
    public void gitTest(@PathVariable String name, @Validated @RequestBody FlowGitTest body) {
        Flow flow = flowService.get(name);
        String gitUrl = body.getGitUrl();

        if (body.hasCredential()) {
            gitConnService.testConn(flow, gitUrl, body.getSecret());
            return;
        }

        if (body.hasPrivateKey()) {
            gitConnService.testConn(flow, gitUrl, body.getRsa());
            return;
        }

        if (body.hasUsernamePassword()) {
            gitConnService.testConn(flow, gitUrl, body.getAuth());
        }

        gitConnService.testConn(flow, gitUrl, StringHelper.EMPTY);
    }

    @GetMapping(value = "/{name}/git/branches")
    @Action(FlowAction.LIST_BRANCH)
    public List<String> listGitBranches(@PathVariable String name) {
        Flow flow = flowService.get(name);
        return gitConnService.listGitBranch(flow);
    }
}
