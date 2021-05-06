package com.flowci.core.job.domain;

import com.flowci.sm.Context;
import com.flowci.zookeeper.InterLock;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class JobSmContext extends Context {

    private final String jobId;

    private Job job;

    private String yml;

    private Step step;

    private InterLock lock;

    public Job.Status getTargetToJobStatus() {
        String name = this.to.getName();
        return Job.Status.valueOf(name);
    }

    public boolean hasLock() {
        return lock != null;
    }
}
