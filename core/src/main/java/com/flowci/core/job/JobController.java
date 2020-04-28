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

package com.flowci.core.job;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.YmlService;
import com.flowci.core.job.domain.*;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.service.*;
import com.flowci.core.job.domain.ExecutedCmd;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.NotAvailableException;
import com.flowci.tree.NodePath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;

/**
 * @author yang
 */
@RestController
@RequestMapping("/jobs")
public class JobController {

    private static final String DefaultPage = "0";

    private static final String DefaultSize = "20";

    private static final String ParameterLatest = "latest";

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private FlowService flowService;

    @Autowired
    private YmlService ymlService;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobActionService jobActionService;

    @Autowired
    private StepService stepService;

    @Autowired
    private LoggingService loggingService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ArtifactService artifactService;

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
        Flow flow = flowService.get(name);

        if (ParameterLatest.equals(buildNumberOrLatest)) {
            return jobService.getLatest(flow.getId());
        }

        try {
            long buildNumber = Long.parseLong(buildNumberOrLatest);
            return jobService.get(flow.getId(), buildNumber);
        } catch (NumberFormatException e) {
            throw new ArgumentException("Build number must be a integer");
        }
    }

    @GetMapping(value = "/{flow}/{buildNumber}/yml", produces = MediaType.APPLICATION_JSON_VALUE)
    @Action(JobAction.GET_YML)
    public String getYml(@PathVariable String flow, @PathVariable String buildNumber) {
        Job job = get(flow, buildNumber);
        JobYml yml = jobService.getYml(job);
        return Base64.getEncoder().encodeToString(yml.getRaw().getBytes());
    }

    @GetMapping("/{flow}/{buildNumberOrLatest}/steps")
    @Action(JobAction.LIST_STEPS)
    public List<ExecutedCmd> listSteps(@PathVariable String flow,
                                       @PathVariable String buildNumberOrLatest) {
        Job job = get(flow, buildNumberOrLatest);
        return stepService.list(job);
    }

    @GetMapping("/logs/{executedCmdId}")
    @Action(JobAction.GET_STEP_LOG)
    public Page<String> getStepLog(@PathVariable String executedCmdId,
                                   @RequestParam(required = false, defaultValue = "0") int page,
                                   @RequestParam(required = false, defaultValue = "50") int size) {

        return loggingService.read(stepService.get(executedCmdId), PageRequest.of(page, size));
    }

    @GetMapping("/logs/{cmdId}/download")
    @Action(JobAction.DOWNLOAD_STEP_LOG)
    public ResponseEntity<Resource> downloadStepLog(@PathVariable String cmdId) {
        ExecutedCmd cmd = stepService.get(cmdId);
        Resource resource = loggingService.get(cmdId);
        Flow flow = flowService.getById(cmd.getFlowId());

        NodePath path = NodePath.create(cmd.getNodePath());
        String fileName = String.format("%s-#%s-%s.log", flow.getName(), cmd.getBuildNumber(), path.name());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @PostMapping
    @Action(JobAction.CREATE)
    public Job create(@Validated @RequestBody CreateJob data) {
        Flow flow = flowService.get(data.getFlow());
        Yml yml = ymlService.getYml(flow);
        return jobService.create(flow, yml.getRaw(), Trigger.API, data.getInputs());
    }

    @PostMapping("/run")
    @Action(JobAction.RUN)
    public void createAndRun(@Validated @RequestBody CreateJob body) {
        try {
            Flow flow = flowService.get(body.getFlow());
            Yml yml = ymlService.getYml(flow);
            Job job = jobService.create(flow, yml.getRaw(), Trigger.API, body.getInputs());
            jobActionService.toStart(job);
        } catch (NotAvailableException e) {
            Job job = (Job) e.getExtra();
            jobActionService.setJobStatusAndSave(job, Job.Status.FAILURE, e.getMessage());
        }
    }

    @PostMapping("/rerun")
    @Action(JobAction.RUN)
    public void rerun(@Validated @RequestBody RerunJob body) {
        try {
            Job job = jobService.get(body.getJobId());
            Flow flow = flowService.getById(job.getFlowId());
            jobService.rerun(flow, job);
        } catch (NotAvailableException e) {
            Job job = (Job) e.getExtra();
            jobActionService.setJobStatusAndSave(job, Job.Status.FAILURE, e.getMessage());
        }
    }

    @PostMapping("/{flow}/{buildNumber}/cancel")
    @Action(JobAction.CANCEL)
    public Job cancel(@PathVariable String flow, @PathVariable String buildNumber) {
        Job job = get(flow, buildNumber);
        jobActionService.toCancel(job);
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

    @GetMapping("/{flow}/{buildNumber}/artifacts")
    @Action(JobAction.LIST_ARTIFACTS)
    public List<JobArtifact> listArtifact(@PathVariable String flow, @PathVariable String buildNumber) {
        Job job = get(flow, buildNumber);
        return artifactService.list(job);
    }

    @GetMapping(value = "/{flow}/{buildNumber}/artifacts/{artifactId}")
    @Action(JobAction.DOWNLOAD_ARTIFACT)
    public ResponseEntity<Resource> downloadArtifact(@PathVariable String flow,
                                @PathVariable String buildNumber,
                                @PathVariable String artifactId) {
        Job job = get(flow, buildNumber);
        JobArtifact artifact = artifactService.fetch(job, artifactId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + artifact.getFileName() + "\"")
                .body(new InputStreamResource(artifact.getSrc()));
    }
}
