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
import com.flow.platform.api.service.JobService;
import com.flow.platform.util.Logger;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yh@firim
 */

@RestController
@RequestMapping(path = "/jobs")
public class JobController {

    private static Logger LOGGER = new Logger(JobController.class);

    @Autowired
    private JobService jobService;

    @PostMapping
    public Job create(@RequestBody String body) {
        return jobService.createJob(body);
    }

    @GetMapping
    public Collection<Job> index(@RequestParam(required = false) String flowPath) {
        return jobService.listJobs(flowPath, null);
    }

    @GetMapping(path = "/{id}")
    public Job show(@PathVariable BigInteger id) {
        return jobService.find(id);
    }

    @PostMapping(path = "/status/latest")
    public Collection<Job> latestStatus(@RequestBody List<String> flowPaths) {
        return jobService.listJobs(null, flowPaths);
    }
}
