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
import com.flowci.core.common.domain.Mongoable;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.agent.domain.Agent;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import com.flowci.store.Pathable;
import com.flowci.tree.Selector;
import com.flowci.util.StringHelper;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author yang
 */
@Getter
@Setter
@Document(collection = "job")
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
        TAG
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
    public static class AgentInfo {

        private String name;

        private String os;

        private int cpu;

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

    private static final Integer MinPriority = 1;

    private static final Integer MaxPriority = 255;

    /**
     * Job key is generated from {flow id}-{build number}
     */
    @Indexed(name = "index_job_key", unique = true)
    private String key;

    @Indexed(name = "index_flow_id", sparse = true)
    private String flowId;

    private String flowName;

    private Long buildNumber;

    private Trigger trigger;

    private Status status = Status.PENDING;

    private Selector agentSelector;

    private String agentId;

    private AgentInfo agentInfo = new AgentInfo();

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
     * Date that job will expired at
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
    public boolean isCancelled() {
        return status == Status.CANCELLED;
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
        if (Objects.isNull(this.startAt)) {
            return StringHelper.EMPTY;
        }
        return DateFormat.format(this.finishAt);
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

    public void setErrorToContext(String err) {
        context.put(Variables.Job.Error, err);
    }

    public void setAgentSnapshot(Agent agent) {
        agentInfo.setName(agent.getName());
        agentInfo.setOs(agent.getOs().name());
        agentInfo.setCpu(agent.getResource().getCpu());
        agentInfo.setTotalMemory(agent.getResource().getTotalMemory());
        agentInfo.setFreeMemory(agent.getResource().getFreeMemory());
        agentInfo.setTotalDisk(agent.getResource().getTotalDisk());
        agentInfo.setFreeDisk(agent.getResource().getFreeDisk());
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
