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

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.service.job.JobService;
import com.flow.platform.api.util.I18nUtil;
import com.flow.platform.api.util.PathUtil;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
public class JobController extends NodeController {

    @Autowired
    private JobService jobService;

    @ModelAttribute
    public void setLocale(@RequestParam(required = false) String locale) {
        if (locale == null) {
            I18nUtil.initLocale("en", "US");
            return;
        }

        if (locale.equals("zh-CN")) {
            I18nUtil.initLocale("zh", "CN");
        }

        if (locale.equals("en-US")) {
            I18nUtil.initLocale("en", "US");
        }
    }

    @PostMapping(path = "/{root}")
    public Job create() {
        String path = getNodePathFromUrl();
        return jobService.createJob(path);
    }

    @GetMapping(path = "/{root}")
    public Collection<Job> index() {
        String path = getNodePathFromUrl();

        List<String> paths = null;
        if (path != null) {
            paths = Lists.newArrayList(path);
        }

        return jobService.list(paths, false);
    }

    @GetMapping(path = "/{root}/{buildNumber}")
    public Job show(@PathVariable Integer buildNumber) {
        String path = getNodePathFromUrl();
        return jobService.find(path, buildNumber);
    }

    @GetMapping(path = "/{root}/{buildNumber}/nodes")
    public List<NodeResult> indexNodeResults(@PathVariable Integer buildNumber) {
        String path = getNodePathFromUrl();
        return jobService.listNodeResult(path, buildNumber);
    }

    @PostMapping(path = "/status/latest")
    public Collection<Job> latestStatus(@RequestBody List<String> paths) {
        return jobService.list(paths, true);
    }

    @PostMapping(path = "/{root}/{buildNumber}/stop")
    public Boolean stopJob(@PathVariable Integer buildNumber) {
        String path = getNodePathFromUrl();
        return jobService.stopJob(path, buildNumber);
    }
}
