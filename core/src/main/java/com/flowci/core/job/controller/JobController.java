/*
 * Copyright 2018 flow.ci
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
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.service.YmlService;
import com.flowci.core.job.domain.*;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.service.ReportService;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author yang
 */
@RestController
@RequestMapping("/jobs")
public class JobController extends BaseController {

    private final SessionManager sessionManager;

    private final YmlService ymlService;

    private final ReportService reportService;

    private final TaskExecutor appTaskExecutor;

    public JobController(SessionManager sessionManager,
                         YmlService ymlService,
                         ReportService reportService,
                         TaskExecutor appTaskExecutor) {
        this.sessionManager = sessionManager;
        this.ymlService = ymlService;
        this.reportService = reportService;
        this.appTaskExecutor = appTaskExecutor;
    }

    @PostMapping("/batch/latest")
    @Action(JobAction.LIST)
    public List<Job> listLatest(@RequestBody Collection<String> flowIdList) {
        if (flowIdList.isEmpty()) {
            return Collections.emptyList();
        }
        return jobService.listLatest(flowIdList);
    }

    @GetMapping("/{flow}")
    @Action(JobAction.LIST)
    public Page<JobItem> list(@PathVariable("flow") String name,
                              @RequestParam(required = false, defaultValue = DefaultPage) int page,
                              @RequestParam(required = false, defaultValue = DefaultSize) int size) {

        Flow flow = flowService.get(name);
        return jobService.list(flow, page, size);
    }

    @GetMapping("/{flow}/{buildNumberOrLatest}")
    @Action(JobAction.GET)
    public Job get(@PathVariable("flow") String name, @PathVariable String buildNumberOrLatest) {
        return super.getJob(name, buildNumberOrLatest);
    }

    @GetMapping("/{jobId}/desc")
    @Action(JobAction.GET)
    public JobDesc getDesc(@PathVariable String jobId) {
        return jobService.getDesc(jobId);
    }

    @GetMapping(value = "/{flow}/{buildNumber}/yml", produces = MediaType.APPLICATION_JSON_VALUE)
    @Action(JobAction.GET_YML)
    public JobYml getYml(@PathVariable String flow, @PathVariable String buildNumber) {
        Job job = get(flow, buildNumber);
        return jobService.getYml(job);
    }

    @GetMapping("/{flow}/{buildNumberOrLatest}/steps")
    @Action(JobAction.LIST_STEPS)
    public List<Step> steps(@PathVariable String flow, @PathVariable String buildNumberOrLatest) {
        Job job = get(flow, buildNumberOrLatest);
        return stepService.list(job);
    }

    @GetMapping("/{flow}/{buildNumberOrLatest}/related")
    @Action(JobAction.LIST)
    public List<JobDesc> relatedJobs(@PathVariable String flow, @PathVariable String buildNumberOrLatest) {
        Job job = get(flow, buildNumberOrLatest);
        return jobService.listRelated(job);
    }

    @PostMapping
    @Action(JobAction.CREATE)
    public Job create(@Validated @RequestBody CreateJob data) {
        var flow = flowService.get(data.getFlow());
        var ymlEntity = ymlService.get(flow.getId());
        return jobService.create(flow, ymlEntity.getList(), Trigger.API, data.getInputs());
    }

    @PostMapping("/run")
    @Action(JobAction.RUN)
    public void createAndStart(@Validated @RequestBody CreateJob body) {
        var current = sessionManager.get();
        var flow = flowService.get(body.getFlow());
        var ymlEntity = ymlService.get(flow.getId());

        // start from thread since could be loading yaml from git repo
        appTaskExecutor.execute(() -> {
            sessionManager.set(current);
            Job job = jobService.create(flow, ymlEntity.getList(), Trigger.API, body.getInputs());
            jobService.start(job);
        });
    }

    @PostMapping("/rerun")
    @Action(JobAction.RUN)
    public void rerun(@Validated @RequestBody RerunJob body) {
        Job job = jobService.get(body.getJobId());
        Flow flow = flowService.getById(job.getFlowId());

        if (body.isFromFailureStep()) {
            jobService.rerunFromFailureStep(flow, job);
            return;
        }

        jobService.rerun(flow, job);
    }

    @PostMapping("/{flow}/{buildNumber}/cancel")
    @Action(JobAction.CANCEL)
    public Job cancel(@PathVariable String flow, @PathVariable String buildNumber) {
        Job job = get(flow, buildNumber);
        jobService.cancel(job);
        return job;
    }

    @GetMapping("/{flow}/{buildNumber}/reports")
    @Action(JobAction.LIST_REPORTS)
    public List<JobReport> listReports(@PathVariable String flow, @PathVariable String buildNumber) {
        Job job = get(flow, buildNumber);
        return reportService.list(job);
    }

    @GetMapping(value = "/{flow}/{buildNumber}/reports/{reportId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Action(JobAction.FETCH_REPORT)
    public String fetchReport(@PathVariable String flow,
                              @PathVariable String buildNumber,
                              @PathVariable String reportId) {
        Job job = get(flow, buildNumber);
        return reportService.fetch(job, reportId);
    }
}
