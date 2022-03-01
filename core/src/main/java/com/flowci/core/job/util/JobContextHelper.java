package com.flowci.core.job.util;

import com.flowci.core.common.domain.Variables;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.domain.Job;

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
}
