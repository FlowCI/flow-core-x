package com.flowci.core.job.util;

import com.flowci.core.common.domain.Variables;
import com.flowci.core.job.domain.Job;

public abstract class JobContextHelper {

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
        return job.getContext().get(Variables.Flow.GitCredential);
    }

    public static String getGitUrl(Job job) {
        return job.getContext().get(Variables.Flow.GitUrl);
    }
}
