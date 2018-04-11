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

package com.flow.platform.api.domain.job;

import com.flow.platform.api.domain.EnvObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yh@firim
 */

@EqualsAndHashCode(of = {"id"}, callSuper = false)
@ToString(of = {"id", "nodePath", "sessionId"})
public class Job extends EnvObject {

    public final static EnumSet<JobStatus> FINISH_STATUS = EnumSet
        .of(JobStatus.FAILURE, JobStatus.STOPPED, JobStatus.SUCCESS, JobStatus.TIMEOUT);

    public final static EnumSet<JobStatus> RUNNING_STATUS = EnumSet
        .of(JobStatus.YML_LOADING, JobStatus.RUNNING, JobStatus.SESSION_CREATING, JobStatus.CREATED);

    public final static EnumSet<JobStatus> FAILURE_STATUS = EnumSet
        .of(JobStatus.FAILURE, JobStatus.STOPPED, JobStatus.TIMEOUT);

    public final static EnumSet<JobStatus> SUCCESS_STATUS = EnumSet
        .of(JobStatus.SUCCESS);

    @Getter
    @Setter
    private BigInteger id;

    @Expose
    @Getter
    @Setter
    private String nodePath;

    @Expose
    @Getter
    @Setter
    private Long number;

    @Getter
    @Setter
    private String sessionId;

    @Expose
    @Getter
    @Setter
    private String nodeName;

    @Expose
    @Getter
    @Setter
    private JobCategory category = JobCategory.MANUAL;

    @Expose
    @Getter
    @Setter
    private JobStatus status = JobStatus.CREATED;

    @Expose
    @Getter
    @Setter
    private ZonedDateTime createdAt;

    @Expose
    @Getter
    @Setter
    private ZonedDateTime updatedAt;

    @Getter
    @Setter
    private String logPath;

    @Expose
    @Getter
    @Setter
    private String failureMessage;

    /**
     * The root node result for job detail
     */
    @Expose
    @Getter
    @Setter
    @SerializedName("result")
    private NodeResult rootResult;

    /**
     * The node result list expect root result
     */
    @Expose
    @Getter
    @Setter
    private List<NodeResult> childrenResult;

    @Expose
    @Getter
    @Setter
    private String createdBy;

    public Job(BigInteger id) {
        this.id = id;
    }

    public Job() {
    }
}