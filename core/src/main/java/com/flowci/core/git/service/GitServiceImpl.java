package com.flowci.core.git.service;

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.git.client.GitAPIClient;
import com.flowci.core.git.domain.GitCommitStatus;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.JobFinishedEvent;
import com.flowci.core.job.util.JobContextHelper;
import com.flowci.util.StringHelper;
import lombok.extern.log4j.Log4j2;
import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Service
public class GitServiceImpl implements GitService {

    @Autowired
    private GitAPIClient githubClient;

    private final Map<GitSource, GitAPIClient> clients = new HashMap<>(5);

    @PostConstruct
    public void init() {
        clients.put(GitSource.GITHUB, githubClient);
    }

    @EventListener
    public void onJobFinishEvent(JobFinishedEvent event) {
        Job job = event.getJob();

        String gitSourceStr = JobContextHelper.getGitSource(job);
        if (!StringHelper.hasValue(gitSourceStr)) {
            log.info("no git source on job {} - {}", job.getFlowName(), job.getBuildNumber());
            return;
        }

        var commit = new GitCommitStatus();
        commit.setId(JobContextHelper.getCommitId(job));
        commit.setUrl(JobContextHelper.getGitUrl(job));
        commit.setTargetUrl(JobContextHelper.getJobUrl(job));
        commit.setStatus(JobContextHelper.getStatus(job).name().toLowerCase());
        commit.setDesc(String.format("build %s from flow.ci", commit.getStatus()));

        try {
            var source = GitSource.valueOf(gitSourceStr);
            var apiClient = clients.get(source);

            if (apiClient != null) {
                apiClient.writeCommitStatus(commit);
            }
        } catch (Throwable e) {
            log.warn(e.getMessage());
        }
    }
}
