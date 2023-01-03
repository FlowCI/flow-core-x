/*
 * Copyright 2020 flow.ci
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

package com.flowci.core.job.controller;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.domain.JobAction;
import com.flowci.core.job.domain.Step;
import com.flowci.core.job.service.LoggingService;
import com.flowci.tree.NodePath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/jobs/logs")
public class LoggingController extends BaseController {

    @Autowired
    private LoggingService loggingService;

    @GetMapping("/{stepId}/read")
    @Action(JobAction.DOWNLOAD_STEP_LOG)
    public Collection<byte[]> readStepLog(@PathVariable String stepId) {
        return loggingService.read(stepId);
    }

    @GetMapping("/{stepId}/download")
    @Action(JobAction.DOWNLOAD_STEP_LOG)
    public ResponseEntity<Resource> downloadStepLog(@PathVariable String stepId) {
        Step step = stepService.get(stepId);
        Resource resource = loggingService.get(stepId);
        Flow flow = flowService.getById(step.getFlowId());

        NodePath path = NodePath.create(step.getNodePath());
        String fileName = String.format("%s-#%s-%s.log", flow.getName(), step.getBuildNumber(), path.name());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
