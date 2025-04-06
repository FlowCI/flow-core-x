package com.flowci.build.business.impl;

import com.flowci.build.business.WaitForAgent;
import com.flowci.build.model.Build;
import com.flowci.build.model.Job;
import com.flowci.build.repo.BuildRepo;
import com.flowci.build.repo.JobRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class WaitForAgentImpl implements WaitForAgent {

    private final BuildRepo buildRepo;
    private final JobRepo jobRepo;

    @Override
    @Transactional
    public void invoke(Long buildId) {
        buildRepo.updateBuildStatusById(buildId, Build.Status.QUEUED);
        jobRepo.updateJobStatusByBuildId(buildId, Job.Status.QUEUED);
    }
}
