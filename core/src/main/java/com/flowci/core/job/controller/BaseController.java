package com.flowci.core.job.controller;

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.service.JobService;
import com.flowci.core.job.service.StepService;
import com.flowci.exception.ArgumentException;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseController {

    protected static final String DefaultPage = "0";

    protected static final String DefaultSize = "20";

    protected static final String ParameterLatest = "latest";

    @Autowired
    protected FlowService flowService;

    @Autowired
    protected JobService jobService;

    @Autowired
    protected StepService stepService;

    protected Job getJob(String id) {
        return jobService.get(id);
    }

    protected Job getJob(String name, String buildNumberOrLatest) {
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
}
