/*
 * Copyright 2022 flow.ci
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

package com.flowci.core.job.util;

import com.flowci.common.helper.StringHelper;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.domain.Job;

import static com.flowci.core.common.domain.Variables.Git.BRANCH;

public abstract class JobContextHelper {

    public static void setServerUrl(Job job, String serverUrl) {
        job.getContext().put(Variables.App.ServerUrl, serverUrl);
    }

    public static void setFlowName(Job job, Flow flow) {
        job.getContext().put(Variables.Flow.Name, flow.getName());
        job.getContext().put(Variables.Git.REPO_NAME, flow.getName());
    }

    public static void setTrigger(Job job, Job.Trigger trigger) {
        job.getContext().put(Variables.Job.Trigger, trigger.name());
    }

    public static void setStatus(Job job, String status) {
        job.getContext().put(Variables.Job.Status, Job.Status.valueOf(status).name());
    }

    public static void setStatus(Job job, Job.Status status) {
        job.getContext().put(Variables.Job.Status, status.name());
    }

    public static Job.Status getStatus(Job job) {
        return Job.Status.valueOf(job.getContext().get(Variables.Job.Status));
    }

    public static String getError(Job job) {
        return job.getContext().get(Variables.Job.Error);
    }

    public static void setError(Job job, String err) {
        job.getContext().put(Variables.Job.Error, err);
    }

    public static String getSecretName(Job job) {
        return job.getContext().get(Variables.Git.SECRET);
    }

    public static String getGitUrl(Job job) {
        return job.getContext().get(Variables.Git.URL);
    }

    public static void setBuildNumber(Job job, Long buildNumber) {
        job.getContext().put(Variables.Job.BuildNumber, String.valueOf(buildNumber));
    }

    public static void setStartAt(Job job, String startAtInStr) {
        job.getContext().put(Variables.Job.StartAt, startAtInStr);
    }

    public static void setFinishAt(Job job, String finishAtInStr) {
        job.getContext().put(Variables.Job.FinishAt, finishAtInStr);
    }

    public static void setDurationInSecond(Job job, String duration) {
        job.getContext().put(Variables.Job.DurationInSeconds, duration);
    }

    public static void setJobUrl(Job job, String url) {
        job.getContext().put(Variables.Job.Url, url);
    }

    public static String getJobUrl(Job job) {
        return job.getContext().get(Variables.Job.Url);
    }

    public static String getCommitId(Job job) {
        return job.getContext().get(Variables.Git.COMMIT_ID);
    }

    public static String getGitSource(Job job) {
        return job.getContext().get(Variables.Git.SOURCE);
    }

    public static String getGitMessage(Job job) {
        String variable = Variables.Git.PUSH_MESSAGE;

        switch (job.getTrigger()) {
            case PR_MERGED:
            case PR_OPENED:
                variable = Variables.Git.PR_MESSAGE;
                break;
            case PATCHSET:
                variable = Variables.Git.PATCHSET_MESSAGE;
        }

        return job.getContext().get(variable, StringHelper.EMPTY);
    }

    public static String getGitBranch(Job job) {
        return job.getContext().get(BRANCH);
    }

    public static String getRepoId(Job job)  {
        return job.getContext().get(Variables.Git.REPO_ID, StringHelper.EMPTY);
    }
}
