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

package com.flowci.core.job.service;

import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.service.SettingService;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.dao.*;
import com.flowci.core.job.domain.*;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.event.JobCreatedEvent;
import com.flowci.core.job.event.JobDeletedEvent;
import com.flowci.core.job.manager.JobActionManager;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.user.domain.User;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.store.FileManager;
import com.flowci.tree.FlowNode;
import com.flowci.util.StringHelper;
import com.google.common.collect.Maps;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static com.flowci.core.trigger.domain.Variables.GIT_AUTHOR;
import static com.flowci.core.trigger.domain.Variables.GIT_COMMIT_ID;

/**
 * @author yang
 */
@Log4j2
@Service
public class JobServiceImpl implements JobService {

    private static final Sort SortByBuildNumber = Sort.by(Direction.DESC, "buildNumber");

    //====================================================================
    //        %% Spring injection
    //====================================================================

    @Autowired
    private JobDao jobDao;

    @Autowired
    private JobPriorityDao jobPriorityDao;

    @Autowired
    private JobAgentDao jobAgentDao;

    @Autowired
    private JobDescDao jobDescDao;

    @Autowired
    private JobItemDao jobItemDao;

    @Autowired
    private JobNumberDao jobNumberDao;

    @Autowired
    private TaskExecutor appTaskExecutor;

    @Autowired
    private YmlManager ymlManager;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private SpringEventManager eventManager;

    @Qualifier("fileManager")
    @Autowired
    private FileManager fileManager;

    @Autowired
    private JobActionManager jobActionManager;

    @Autowired
    private StepService stepService;

    @Autowired
    private LocalTaskService localTaskService;

    @Autowired
    private SettingService settingService;

    //====================================================================
    //        %% Public functions
    //====================================================================

    @Override
    public Job get(String jobId) {
        Optional<Job> job = jobDao.findById(jobId);

        if (job.isPresent()) {
            return job.get();
        }

        throw new NotFoundException("Job {0} not found", jobId);
    }

    @Override
    public JobDesc getDesc(String id) {
        Optional<JobDesc> desc = jobDescDao.findById(id);
        return desc.orElse(null);
    }

    @Override
    public Job get(String flowId, Long buildNumber) {
        String key = JobKey.of(flowId, buildNumber).toString();
        Optional<Job> optional = jobDao.findByKey(key);

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new NotFoundException(
                "The flow {0} for build number {1} cannot found", flowId, Long.toString(buildNumber));
    }

    @Override
    public JobYml getYml(Job job) {
        return ymlManager.get(job);
    }

    @Override
    public Job getLatest(String flowId) {
        Optional<JobNumber> optional = jobNumberDao.findById(flowId);

        if (optional.isPresent()) {
            JobNumber latest = optional.get();
            return get(flowId, latest.getNumber());
        }

        throw new NotFoundException("No jobs for flow {0}", flowId);
    }

    @Override
    public Page<JobItem> list(Flow flow, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, SortByBuildNumber);
        return jobItemDao.findAllByFlowId(flow.getId(), pageable);
    }

    @Override
    public Job create(Flow flow, String yml, Trigger trigger, StringVars input) {
        Job job = createJob(flow, trigger, input);
        eventManager.publish(new JobCreatedEvent(this, job));

        if (job.isYamlFromRepo()) {
            jobActionManager.toLoading(job);
            return job;
        }

        if (!StringHelper.hasValue(yml)) {
            throw new ArgumentException("YAML config is required to start a job");
        }

        jobActionManager.toCreated(job, yml);
        return job;
    }

    @Override
    public void start(Job job) {
        jobActionManager.toStart(job);
    }

    @Override
    public void cancel(Job job) {
        jobActionManager.toCancelled(job, null);
    }

    @Override
    public Job rerun(Flow flow, Job job) {
        if (!job.isDone()) {
            throw new StatusException("Job not finished, cannot re-start");
        }

        // load yaml
        JobYml yml = ymlManager.get(job);
        FlowNode root = ymlManager.parse(yml.getRaw());

        // reset
        job.setTimeout(flow.getStepTimeout());
        job.setExpire(flow.getStepTimeout());
        job.setCreatedAt(Date.from(Instant.now()));
        job.setFinishAt(null);
        job.setStartAt(null);
        job.setSnapshots(Maps.newHashMap());
        job.setStatus(Job.Status.PENDING);
        job.setTrigger(Trigger.MANUAL);
        job.resetCurrentPath();
        job.setPriority(Job.MaxPriority);
        job.setCreatedBy(sessionManager.getUserEmail());

        // re-init job context
        Vars<String> context = job.getContext();
        String lastCommitId = context.get(GIT_COMMIT_ID);
        context.clear();

        initJobContext(job, flow, null);
        context.put(GIT_COMMIT_ID, lastCommitId);
        context.put(Variables.Job.TriggerBy, sessionManager.get().getEmail());
        context.merge(root.getEnvironments(), false);

        // reset job agent
        jobAgentDao.save(new JobAgent(job.getId(), flow.getId()));

        // cleanup
        stepService.delete(job);
        localTaskService.delete(job);
        ymlManager.delete(job);

        jobActionManager.toCreated(job, yml.getRaw());
        jobActionManager.toStart(job);
        return job;
    }

    @Override
    public Job rerunFromFailureStep(Flow flow, Job job) {
        if (!job.isDone()) {
            throw new StatusException("Job not finished, cannot re-start");
        }

        if (!job.isFailure()) {
            throw new StatusException("Job is not failure status, cannot re-start from failure step");
        }

        // reset job properties
        job.setFinishAt(null);
        job.setStartAt(null);
        job.setExpire(flow.getStepTimeout());
        job.setSnapshots(Maps.newHashMap());
        job.setPriority(Job.MaxPriority);
        job.setStatus(Job.Status.CREATED);
        job.setTrigger(Trigger.MANUAL);
        job.setCreatedBy(sessionManager.getUserEmail());

        // reset job agent
        jobAgentDao.save(new JobAgent(job.getId(), flow.getId()));

        jobActionManager.toStart(job);
        return job;
    }

    @Override
    public void delete(Flow flow) {
        appTaskExecutor.execute(() -> {
            jobNumberDao.deleteAllByFlowId(flow.getId());
            log.info("Deleted: job number of flow {}", flow.getName());

            jobPriorityDao.deleteAllByFlowId(flow.getId());
            log.info("Deleted: job priority of flow {}", flow.getName());

            jobAgentDao.deleteAllByFlowId(flow.getId());
            log.info("Deleted: job agent of flow {}", flow.getName());

            Long numOfJobDeleted = jobDao.deleteAllByFlowId(flow.getId());
            log.info("Deleted: {} jobs of flow {}", numOfJobDeleted, flow.getName());

            Long numOfStepDeleted = stepService.delete(flow);
            log.info("Deleted: {} steps of flow {}", numOfStepDeleted, flow.getName());

            Long numOfTaskDeleted = localTaskService.delete(flow);
            log.info("Deleted: {} tasks of flow {}", numOfTaskDeleted, flow.getName());

            eventManager.publish(new JobDeletedEvent(this, flow, numOfJobDeleted));
        });
    }

    //====================================================================
    //        %% Utils
    //====================================================================

    private Job createJob(Flow flow, Trigger trigger, Vars<String> input) {
        // create job number
        JobNumber jobNumber = jobNumberDao.increaseBuildNumber(flow.getId());

        // create job
        Job job = new Job();
        job.setKey(JobKey.of(flow.getId(), jobNumber.getNumber()).toString());
        job.setFlowId(flow.getId());
        job.setFlowName(flow.getName());
        job.setTrigger(trigger);
        job.setBuildNumber(jobNumber.getNumber());
        job.setTimeout(flow.getStepTimeout());
        job.setExpire(flow.getJobTimeout());
        job.setYamlFromRepo(flow.isYamlFromRepo());
        job.setYamlRepoBranch(flow.getYamlRepoBranch());

        // init job context
        initJobContext(job, flow, input);

        setTriggerBy(job);

        // create job file space
        try {
            fileManager.create(flow, job);
        } catch (IOException e) {
            throw new StatusException("Cannot create workspace for job: {0}", e.getMessage());
        }

        // create job priority
        Optional<JobPriority> jobPriorityOptional = jobPriorityDao.findByFlowId(flow.getId());
        if (!jobPriorityOptional.isPresent()) {
            JobPriority jobPriority = new JobPriority();
            jobPriority.setFlowId(flow.getId());
            jobPriorityDao.save(jobPriority);
        }

        jobDao.insert(job);
        jobAgentDao.save(new JobAgent(job.getId(), flow.getId()));
        return job;
    }

    // setup created by form login user or git event author
    private void setTriggerBy(Job job) {
        Vars<String> context = job.getContext();
        if (sessionManager.exist()) {
            context.put(Variables.Job.TriggerBy, sessionManager.getUserEmail());
            return;
        }

        if (job.getTrigger() == Trigger.SCHEDULER) {
            context.put(Variables.Job.TriggerBy, User.DefaultSystemUser);
            return;
        }

        String createdBy = context.get(GIT_AUTHOR, "Unknown");
        job.setCreatedBy(createdBy);
        context.put(Variables.Job.TriggerBy, createdBy);
    }

    private void initJobContext(Job job, Flow flow, Vars<String> inputs) {
        StringVars context = new StringVars();
        context.mergeFromTypedVars(flow.getLocally());

        context.put(Variables.App.ServerUrl, settingService.get().getServerUrl());

        context.put(Variables.Flow.Name, flow.getName());
        context.put(Variables.Flow.GitRepo, flow.getName());

        context.put(Variables.Job.Status, Job.Status.PENDING.name());
        context.put(Variables.Job.Trigger, job.getTrigger().toString());
        context.put(Variables.Job.BuildNumber, job.getBuildNumber().toString());
        context.put(Variables.Job.StartAt, job.startAtInStr());
        context.put(Variables.Job.FinishAt, job.finishAtInStr());

        if (!Objects.isNull(inputs)) {
            context.merge(inputs);
        }

        job.getContext().merge(context);
    }
}
