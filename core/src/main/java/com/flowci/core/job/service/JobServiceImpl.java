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

import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.git.GitClient;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.dao.JobItemDao;
import com.flowci.core.job.dao.JobNumberDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.domain.JobItem;
import com.flowci.core.job.domain.JobNumber;
import com.flowci.core.job.domain.JobYml;
import com.flowci.core.job.event.JobCreatedEvent;
import com.flowci.core.job.event.JobDeletedEvent;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.job.util.JobKeyBuilder;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.service.SecretService;
import com.flowci.domain.SimpleSecret;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import com.flowci.exception.NotAvailableException;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.store.FileManager;
import com.flowci.tree.FlowNode;
import com.flowci.tree.YmlParser;
import com.flowci.util.StringHelper;
import com.google.common.base.Strings;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
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
    private String serverUrl;

    @Autowired
    private ConfigProperties.Job jobProperties;

    @Autowired
    private Path repoDir;

    @Autowired
    private Path tmpDir;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private JobItemDao jobItemDao;

    @Autowired
    private JobNumberDao jobNumberDao;

    @Autowired
    private ThreadPoolTaskExecutor jobDeleteExecutor;

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
    private JobActionService jobStateService;

    @Autowired
    private StepService stepService;

    @Autowired
    private SecretService secretService;

    //====================================================================
    //        %% Public functions
    //====================================================================

    @Override
    public Job get(String jobId) {
        Optional<Job> job = jobDao.findById(jobId);

        if (job.isPresent()) {
            return job.get();
        }

        throw new NotFoundException("Job '{}' not found", jobId);
    }

    @Override
    public Job get(String flowId, Long buildNumber) {
        String key = JobKeyBuilder.build(flowId, buildNumber);
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
    public synchronized Job create(Flow flow, String yml, Trigger trigger, StringVars input) {
        Job job = createJob(flow, trigger, input);
        eventManager.publish(new JobCreatedEvent(this, job));

        if (job.isYamlFromRepo()) {
            jobStateService.setJobStatusAndSave(job, Job.Status.LOADING, StringHelper.EMPTY);
            yml = fetchYamlFromGit(flow.getName(), job);
        }

        setupYaml(flow, yml, job);
        stepService.init(job);
        jobStateService.setJobStatusAndSave(job, Job.Status.CREATED, StringHelper.EMPTY);
        return job;
    }

    @Override
    public Job rerun(Flow flow, Job job) {
        if (!Job.FINISH_STATUS.contains(job.getStatus())) {
            throw new StatusException("Job not finished, cannot re-start");
        }

        // load yaml
        JobYml yml = ymlManager.get(job);
        FlowNode root = YmlParser.load(flow.getName(), yml.getRaw());

        // reset
        setExpireTime(job);
        job.setCreatedAt(Date.from(Instant.now()));
        job.setFinishAt(null);
        job.setStartAt(null);
        job.setAgentId(null);
        job.setAgentInfo(null);
        job.setTrigger(Trigger.MANUAL);
        job.setCurrentPath(root.getPathAsString());
        job.setCreatedBy(sessionManager.getUserId());

        // re-init job context
        Vars<String> context = job.getContext();
        String lastCommitId = context.get(GIT_COMMIT_ID);
        context.clear();

        initJobContext(job, flow, null);
        context.put(GIT_COMMIT_ID, lastCommitId);
        context.put(Variables.Job.TriggerBy, sessionManager.get().getEmail());
        context.merge(root.getEnvironments(), false);

        jobStateService.setJobStatusAndSave(job, Job.Status.CREATED, StringHelper.EMPTY);

        // init steps
        stepService.delete(job);
        stepService.init(job);

        jobStateService.start(job);
        return job;
    }

    @Override
    public void delete(Flow flow) {
        jobDeleteExecutor.execute(() -> {
            jobNumberDao.deleteByFlowId(flow.getId());
            log.info("Deleted: job number of flow {}", flow.getName());

            Long numOfJobDeleted = jobDao.deleteByFlowId(flow.getId());
            log.info("Deleted: {} jobs of flow {}", numOfJobDeleted, flow.getName());

            Long numOfStepDeleted = stepService.delete(flow);
            log.info("Deleted: {} steps of flow {}", numOfStepDeleted, flow.getName());

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
        job.setKey(JobKeyBuilder.build(flow.getId(), jobNumber.getNumber()));
        job.setFlowId(flow.getId());
        job.setTrigger(trigger);
        job.setBuildNumber(jobNumber.getNumber());
        job.setCreatedAt(Date.from(Instant.now()));
        job.setTimeout(jobProperties.getTimeoutInSeconds());
        job.setExpire(jobProperties.getExpireInSeconds());
        job.setYamlFromRepo(flow.isYamlFromRepo());
        job.setYamlRepoBranch(flow.getYamlRepoBranch());
        setExpireTime(job);

        // init job context
        initJobContext(job, flow, input);

        // setup created by form login user or git event author
        if (sessionManager.exist()) {
            job.setCreatedBy(sessionManager.getUserId());
            job.getContext().put(Variables.Job.TriggerBy, sessionManager.get().getEmail());
        } else {
            String createdBy = job.getContext().get(GIT_AUTHOR, "Unknown");
            job.setCreatedBy(createdBy);
            job.getContext().put(Variables.Job.TriggerBy, createdBy);
        }

        // create job file space
        try {
            fileManager.create(flow, job);
        } catch (IOException e) {
            throw new StatusException("Cannot create workspace for job");
        }

        // save
        return jobDao.insert(job);
    }

    private void setExpireTime(Job job) {
        long totalExpire = job.getExpire() + job.getTimeout();
        Instant expireAt = Instant.now().plus(totalExpire, ChronoUnit.SECONDS);
        job.setExpireAt(Date.from(expireAt));
    }

    private void setupYaml(Flow flow, String yml, Job job) {
        FlowNode root = YmlParser.load(flow.getName(), yml);

        job.setCurrentPath(root.getPathAsString());
        job.setAgentSelector(root.getSelector());
        job.getContext().merge(root.getEnvironments(), false);

        ymlManager.create(flow, job, yml);
        jobDao.save(job);
    }

    private String fetchYamlFromGit(String flowName, Job job) {
        final String gitUrl = job.getGitUrl();

        if (!StringHelper.hasValue(gitUrl)) {
            throw new NotAvailableException("Git url is missing").setExtra(job);
        }

        final Path dir = getFlowRepoDir(gitUrl, job.getYamlRepoBranch());

        try {
            GitClient client = new GitClient(gitUrl, tmpDir, getSimpleSecret(job.getCredentialName()));
            client.klone(dir, job.getYamlRepoBranch());
        } catch (Exception e) {
            log.warn("Unable to fetch yaml config for flow {}", flowName, e);
            throw new NotAvailableException("Unable to fetch yaml config for flow {0}", flowName).setExtra(job);
        }

        String[] files = dir.toFile().list((currentDir, fileName) ->
                (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) && fileName.startsWith(".flowci"));

        if (files == null || files.length == 0) {
            throw new NotAvailableException("Unable to find yaml file in repo").setExtra(job);
        }

        try {
            byte[] ymlInBytes = Files.readAllBytes(Paths.get(dir.toString(), files[0]));
            return new String(ymlInBytes);
        } catch (IOException e) {
            throw new NotAvailableException("Unable to read yaml file in repo").setExtra(job);
        }
    }

    /**
     * Get flow repo path: {repo dir}/{flow id}
     */
    private Path getFlowRepoDir(String repoUrl, String branch) {
        String b64 = Base64.getEncoder().encodeToString(repoUrl.getBytes());
        return Paths.get(repoDir.toString(), b64 + "_" + branch);
    }

    private SimpleSecret getSimpleSecret(String credentialName) {
        if (Strings.isNullOrEmpty(credentialName)) {
            return null;
        }

        final Secret secret = secretService.get(credentialName);
        return secret.toSimpleSecret();
    }

    private void initJobContext(Job job, Flow flow, Vars<String> inputs) {
        StringVars context = new StringVars();
        context.mergeFromTypedVars(flow.getLocally());

        context.put(Variables.App.Url, serverUrl);
        context.put(Variables.Flow.Name, flow.getName());

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
