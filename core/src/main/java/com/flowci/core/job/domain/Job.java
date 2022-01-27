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

package com.flowci.core.job.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.core.agent.domain.Agent;
import com.flowci.core.agent.domain.AgentProfile;
import com.flowci.core.common.domain.Mongoable;
import com.flowci.core.common.domain.Variables;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import com.flowci.store.Pathable;
import com.flowci.util.StringHelper;
import com.google.common.collect.ImmutableSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * @author yang
 */
@Document(collection = "job")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@CompoundIndex(
        name = "index_job_flowid_and_buildnum",
        def = "{'flowId': 1, 'buildNumber': 1}",
        unique = true
)
public class Job extends Mongoable implements Pathable {

    public enum Trigger {

        /**
         * Scheduler trigger
         */
        SCHEDULER,

        /**
         * Api trigger
         */
        API,

        /**
         * Manual trigger
         */
        MANUAL,

        /**
         * Git push event
         */
        PUSH,

        /**
         * Git PR opened event
         */
        PR_OPENED,

        /**
         * Git PR merged event
         */
        PR_MERGED,

        /**
         * Git tag event
         */
        TAG,

        /**
         * Git patchset trigger
         */
        PATCHSET
    }

    public enum Status {

        /**
         * Initial job state
         */
        PENDING(0),

        /**
         * Job created with yaml and steps
         */
        CREATED(1),

        /**
         * Loading the yaml from git repo
         */
        LOADING(2),

        /**
         * Been put to job queue
         */
        QUEUED(3),

        /**
         * Agent take over the job, and been start to execute
         */
        RUNNING(4),

        /**
         * Job will be cancelled, but waiting for response from agent
         */
        CANCELLING(5),

        /**
         * Job been executed
         */
        SUCCESS(10),

        /**
         * Job been executed but failure
         */
        FAILURE(10),

        /**
         * Job been cancelled by user
         */
        CANCELLED(10),

        /**
         * Job execution time been over the expiredAt
         */
        TIMEOUT(10);

        @Getter
        private int order;

        Status(int order) {
            this.order = order;
        }
    }

    /**
     * Agent snapshot
     */
    @Getter
    @Setter
    public static class AgentSnapshot {

        private String name;

        private String os;

        private int cpuNum;

        private double cpuUsage;

        private int totalMemory;

        private int freeMemory;

        private int totalDisk;

        private int freeDisk;
    }

    public static Pathable path(Long buildNumber) {
        Job job = new Job();
        job.setBuildNumber(buildNumber);
        return job;
    }

    public static final Set<Status> FINISH_STATUS = ImmutableSet.<Status>builder()
            .add(Status.TIMEOUT)
            .add(Status.CANCELLED)
            .add(Status.FAILURE)
            .add(Status.SUCCESS)
            .build();

    private static final SimpleDateFormat DateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static final Integer MinPriority = 1;

    public static final Integer MaxPriority = 255;

    /**
     * Job key is generated from {flow id}-{build number}
     */
    @Indexed(name = "index_job_key", unique = true)
    private String key;

    @Indexed(name = "index_flow_id", partialFilter = "{ flowId: {$exists: true} }")
    private String flowId;

    private String flowName;

    private Long buildNumber;

    private Trigger trigger;

    private Status status = Status.PENDING;

    private boolean onPostSteps = false;

    // agent id : info
    private Map<String, AgentSnapshot> snapshots = new HashMap<>();

    // current running steps
    private Set<String> currentPath = new HashSet<>();

    private Vars<String> context = new StringVars();

    private String message;

    private Integer priority = MinPriority;

    private boolean isYamlFromRepo;

    private String yamlRepoBranch;

    /**
     * Step timeout in seconds
     */
    private int timeout = 1800;

    /**
     * Timeout while job queuing
     */
    private int expire = 1800;

    /**
     * Date that job will be expired at
     */
    private Date expireAt;

    /**
     * Real execution start at
     */
    private Date startAt;

    /**
     * Real execution finish at
     */
    private Date finishAt;

    private int numOfArtifact = 0;

    public void setExpire(int expire) {
        this.expire = expire;
        Instant expireAt = Instant.now().plus(expire, ChronoUnit.SECONDS);
        this.expireAt = (Date.from(expireAt));
    }

    @JsonIgnore
    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    @JsonIgnore
    public boolean isCancelling() {
        return status == Status.CANCELLING;
    }

    @JsonIgnore
    public boolean isDone() {
        return FINISH_STATUS.contains(status);
    }

    @JsonIgnore
    public boolean isFailure() {
        return status == Status.FAILURE;
    }

    @JsonIgnore
    public String getQueueName() {
        return "flow.q." + flowId + ".job";
    }

    @JsonIgnore
    @Override
    public String pathName() {
        return getBuildNumber().toString();
    }

    @JsonIgnore
    public String startAtInStr() {
        if (Objects.isNull(this.startAt)) {
            return StringHelper.EMPTY;
        }
        return DateFormat.format(this.startAt);
    }

    @JsonIgnore
    public String finishAtInStr() {
        if (Objects.isNull(this.finishAt)) {
            return StringHelper.EMPTY;
        }
        return DateFormat.format(this.finishAt);
    }

    @JsonIgnore
    public Long getDurationInSeconds() {
        if (this.startAt == null || this.finishAt == null) {
            return 0L;
        }
        return Duration.between(this.startAt.toInstant(), this.finishAt.toInstant()).getSeconds();
    }

    @JsonIgnore
    public Status getStatusFromContext() {
        return Job.Status.valueOf(context.get(Variables.Job.Status));
    }

    public void setStatusToContext(Status status) {
        context.put(Variables.Job.Status, status.name());
    }

    @JsonIgnore
    public String getErrorFromContext() {
        return context.get(Variables.Job.Error);
    }

    public String getCredentialName() {
        return context.get(Variables.Flow.GitCredential);
    }

    public String getGitUrl() {
        return context.get(Variables.Flow.GitUrl);
    }

    public boolean isExpired() {
        Instant expireAt = getExpireAt().toInstant();
        return Instant.now().compareTo(expireAt) > 0;
    }

    public void addAgentSnapshot(Agent agent, AgentProfile profile) {
        AgentSnapshot s = new AgentSnapshot();
        s.name = agent.getName();
        s.os = agent.getOs().name();
        s.cpuNum = profile.getCpuNum();
        s.cpuUsage = profile.getCpuUsage();
        s.totalMemory = profile.getTotalMemory();
        s.freeMemory = profile.getFreeMemory();
        s.totalDisk = profile.getTotalDisk();
        s.freeDisk = profile.getFreeDisk();

        this.snapshots.put(agent.getId(), s);
    }

    public void setErrorToContext(String err) {
        context.put(Variables.Job.Error, err);
    }

    public Job resetCurrentPath() {
        this.currentPath.clear();
        return this;
    }

    public void addToCurrentPath(Step step) {
        this.currentPath.add(step.getNodePath());
    }

    public void removeFromCurrentPath(Step step) {
        this.currentPath.remove(step.getNodePath());
    }
}
