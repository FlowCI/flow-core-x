package com.flowci.core.git.service;

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.git.domain.GitCommit;
import com.flowci.core.git.domain.GitCommitStatus;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.JobFinishedEvent;
import com.flowci.util.StringHelper;
import lombok.extern.log4j.Log4j2;
import org.jvnet.hk2.annotations.Service;
import org.springframework.context.event.EventListener;

@Log4j2
@Service
public class GitServiceImpl implements GitService {

    @Override
    public void writeStatus(GitCommitStatus commit) {

    }

    @EventListener
    public void onJobFinishEvent(JobFinishedEvent event) {
        Job job = event.getJob();

        String gitSourceStr = job.getContext().get(Variables.Git.SOURCE);
        if (!StringHelper.hasValue(gitSourceStr)) {
            log.info("no git source on job {} - {}", job.getFlowName(), job.getBuildNumber());
            return;
        }

        var commit = new GitCommitStatus();
//        commit.setId();
//        commit.setTargetUrl(job.getGitUrl());
//        commit.setStatus(job.getStatus().name().toLowerCase());

    }
}
