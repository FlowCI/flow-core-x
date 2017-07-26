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

import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.JobStep;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.service.JobService;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.service.YamlService;
import com.flow.platform.util.Logger;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yh@firim
 */

@RestController
public class JobController {

    private static Logger LOGGER = new Logger(JobController.class);

    @Autowired
    JobService jobService;

    @Autowired
    NodeService nodeService;

    @Autowired
    YamlService yamlService;

//    @PostMapping(path = "/jobs")
//    public Job create(@RequestBody List<Step> steps) {
//        Flow flow = new Flow();
//        flow.setName(UUID.randomUUID().toString());
//        flow.setPath(UUID.randomUUID().toString());
//        for (Step step : steps) {
//            step.setParent(flow);
//            step.setPath(new StringBuffer(flow.getName()).append("/").append(step.getName()).toString());
//        }
//        flow.getChildren().addAll(steps);
//        nodeService.create(flow);
//
//        Job job = jobService.createJob(flow.getPath());
//        return job;
//    }

    @PostMapping(path = "/jobs")
    public Job create(@RequestBody String body) {
        Node node  = yamlService.createNodeByYamlString(body);
        nodeService.create(node);

        Job job = jobService.createJob(node.getPath());
        return job;
    }

    @GetMapping(path = "/steps")
    public Collection<JobStep> showSteps(@RequestParam String jobId) {
        return jobService.listJobStep(jobId);
    }

    @GetMapping(path = "/jobs/{id}")
    public Job show(@PathVariable String id) {
        return jobService.find(id);
    }
}
