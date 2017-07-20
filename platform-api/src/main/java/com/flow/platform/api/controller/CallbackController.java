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

import com.flow.platform.api.service.JobService;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yh@firim
 */

@RestController
@RequestMapping("/callback")
public class CallbackController {

    @Autowired
    private JobService jobService;

    @PostMapping(path = "/{jobId}/createSession")
    public String createSession(@RequestBody CmdBase cmdBase, @PathVariable String jobId) {
        jobService.handleCreateSessionCallBack(cmdBase, jobId);
        return "{\"message\": \"ok\"}";
    }

    @PostMapping(path = "/{nodePath}/message")
    public String handleMessage(@RequestBody CmdBase cmdBase, @PathVariable String nodePath) {
        jobService.handleCmdResult((Cmd) cmdBase, nodePath);
        return "{\"message\": \"ok\"}";
    }

}
