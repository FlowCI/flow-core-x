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

import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.domain.Settings;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.RabbitOperations;
import com.flowci.core.common.service.SettingService;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.dao.*;
import com.flowci.core.job.domain.*;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.event.JobActionEvent;
import com.flowci.core.job.event.JobCreatedEvent;
import com.flowci.core.job.event.JobsDeletedEvent;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.job.util.JobContextHelper;
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

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static com.flowci.core.common.domain.Variables.Git.*;

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
    private AppProperties.RabbitMQ rabbitProperties;

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
    private RelatedJobsDao relatedJobsDao;

    @Autowired
    private TaskExecutor appTaskExecutor;

    @Autowired
    private YmlManager ymlManager;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private RabbitOperations jobsQueueManager;

    @Qualifier("fileManager")
    @Autowired
    private FileManager fileManager;

    @Autowired
    private JobActionService jobActionService;

    @Autowired
    private StepService stepService;

    @Autowired
    private SettingService settingService;

    @PostConstruct
    public void startJobDeadLetterConsumer() throws IOException {
        String deadLetterQueue = rabbitProperties.getJobDlQueue();
        jobsQueueManager.startConsumer(deadLetterQueue, true, (header, body, envelope) -> {
            try {
                String jobId = new String(body);
                eventManager.publish(new JobActionEvent(this, jobId, JobActionEvent.ACTION_TO_TIMEOUT));
            } catch (Exception e) {
                log.warn(e);
            }
            return false;
        }, null);
    }


    //====================================================================
    //        %% Public functions
    //====================================================================

    @Override
    public void init(Flow flow) {
        Optional<JobNumber> optional = jobNumberDao.findByFlowId(flow.getId());
        if (optional.isEmpty()) {
            jobNumberDao.save(new JobNumber(flow.getId()));
        }
        declareJobQueueAndStartConsumer(flow);
    }

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
        Optional<JobNumber> optional = jobNumberDao.findByFlowId(flowId);

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
    public List<JobDesc> listRelated(Job job) {
        String gitEventId = job.getContext().get(EVENT_ID);
        if (!StringHelper.hasValue(gitEventId)) {
            return Collections.emptyList();
        }

        Optional<RelatedJobs> optional = relatedJobsDao.findByGitEventId(gitEventId);
        if (optional.isEmpty()) {
            return Collections.emptyList();
        }

        return optional.get().getJobs();
    }

    @Override
    public Job create(Flow flow, String yml, Trigger trigger, StringVars input) {
        Job job = createJob(flow, trigger, input);
        eventManager.publish(new JobCreatedEvent(this, job));

        if (job.isYamlFromRepo()) {
            jobActionService.toLoading(job.getId());
            return job;
        }

        if (!StringHelper.hasValue(yml)) {
            throw new ArgumentException("YAML config is required to start a job");
        }

        jobActionService.toCreated(job.getId(), yml);
        return get(job.getId());
    }

    @Override
    public void start(Job job) {
        jobActionService.toStart(job.getId());
    }

    @Override
    public void cancel(Job job) {
        jobActionService.toCancelled(job.getId(), null);
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
        job.resetCurrentPath();
        job.setPriority(Job.MaxPriority);
        job.setCreatedBy(sessionManager.getUserEmail());

        // re-init job context
        Vars<String> context = job.getContext();
        Iterator<Map.Entry<String, String>> iterator = context.entrySet().iterator();

        while (iterator.hasNext()) {
            var key = iterator.next().getKey();

            if (PUSH_TAG_VARS.contains(key)) {
                continue;
            }

            if (PR_VARS.contains(key)) {
                continue;
            }

            if (PATCHSET_VARS.contains(key)) {
                continue;
            }

            if (GENERIC_VARS.contains(key)) {
                continue;
            }

            iterator.remove();
        }

        initJobContext(job, flow, null);
        context.put(Variables.Job.TriggerBy, sessionManager.get().getEmail());
        context.merge(root.getEnvironments(), false);


        jobDao.save(job);

        // reset job agent
        jobAgentDao.save(new JobAgent(job.getId(), flow.getId()));

        // cleanup
        stepService.delete(job);
        ymlManager.delete(job);

        jobActionService.toCreated(job.getId(), yml.getRaw());
        jobActionService.toStart(job.getId());
        return get(job.getId());
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

        jobActionService.toStart(job.getId());
        return job;
    }

    @Override
    public void delete(Flow flow) {
        appTaskExecutor.execute(() -> {
            jobsQueueManager.removeConsumer(flow.getQueueName());

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

            eventManager.publish(new JobsDeletedEvent(this, flow, numOfJobDeleted));
        });
    }

    //====================================================================
    //        %% Utils
    //====================================================================

    private void declareJobQueueAndStartConsumer(Flow flow) {
        try {
            final String queue = flow.getQueueName();
            jobsQueueManager.declare(queue, true, 255, rabbitProperties.getJobDlExchange());

            jobsQueueManager.startConsumer(queue, false, (header, body, envelope) -> {
                try {
                    String jobId = new String(body);
                    eventManager.publish(new JobActionEvent(this, jobId, JobActionEvent.ACTION_TO_RUN));
                } catch (Exception e) {
                    log.warn(e);
                }
                return true;
            }, appTaskExecutor);
        } catch (IOException e) {
            log.warn(e);
        }
    }

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

        // update git event related jobs
        String gitEventId = job.getContext().get(EVENT_ID);
        if (StringHelper.hasValue(gitEventId)) {
            relatedJobsDao.addRelatedInfo(gitEventId, JobDesc.create(job));
        }

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

        String createdBy = context.get(new String[]{PUSH_AUTHOR, PR_AUTHOR, PATCHSET_AUTHOR}, "Unknown");
        job.setCreatedBy(createdBy);
        context.put(Variables.Job.TriggerBy, createdBy);
    }

    private void initJobContext(Job job, Flow flow, Vars<String> inputs) {
        StringVars context = new StringVars();
        context.mergeFromTypedVars(flow.getVars());

        Settings settings = settingService.get();
        JobContextHelper.setServerUrl(job, settings.getServerUrl());

        JobContextHelper.setFlowName(job, flow);
        JobContextHelper.setStatus(job, Job.Status.PENDING);
        JobContextHelper.setTrigger(job, job.getTrigger());
        JobContextHelper.setBuildNumber(job, job.getBuildNumber());
        JobContextHelper.setStartAt(job, job.startAtInStr());
        JobContextHelper.setFinishAt(job, job.finishAtInStr());
        JobContextHelper.setDurationInSecond(job, "0");
        JobContextHelper.setJobUrl(job, String.format("%s/#/flows/%s/jobs/%s", settings.getWebUrl(), flow.getName(), job.getBuildNumber()));

        if (!Objects.isNull(inputs)) {
            context.merge(inputs);
        }

        job.getContext().merge(context);
    }
}
