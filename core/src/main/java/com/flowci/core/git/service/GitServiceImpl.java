package com.flowci.core.git.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.git.client.GitAPIClient;
import com.flowci.core.git.client.GithubAPIClient;
import com.flowci.core.git.dao.GitConfigDao;
import com.flowci.core.git.domain.GitCommitStatus;
import com.flowci.core.git.domain.GitConfig;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.JobFinishedEvent;
import com.flowci.core.job.util.JobContextHelper;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.domain.TokenSecret;
import com.flowci.core.secret.event.GetSecretEvent;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.UnsupportedException;
import com.flowci.util.StringHelper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.http.HttpClient;
import java.util.*;

@Log4j2
@Service("gitService")
public class GitServiceImpl implements GitService {

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GitConfigDao gitConfigDao;

    @Autowired
    private SpringEventManager eventManager;

    private final Map<GitSource, OperationHandler> handlers = new HashMap<>(5);

    @PostConstruct
    public void init() {
        handlers.put(GitSource.GITHUB, new GitHubOperationHandler());
    }

    @Override
    public List<GitConfig> list() {
        return gitConfigDao.findAll();
    }

    @Override
    public GitConfig get(GitSource source) {
        Optional<GitConfig> optional = gitConfigDao.findBySource(source);
        if (optional.isEmpty()) {
            throw new NotFoundException("Git config not found");
        }
        return optional.get();
    }

    @Override
    public GitConfig save(GitConfig config) {
        OperationHandler handler = getHandler(config.getSource());
        return handler.save(config);
    }

    @Override
    public void delete(GitSource source) {
        gitConfigDao.deleteBySource(source);
    }

    @EventListener
    public void onJobFinishEvent(JobFinishedEvent event) {
        Job job = event.getJob();

        String gitSourceStr = JobContextHelper.getGitSource(job);
        if (!StringHelper.hasValue(gitSourceStr)) {
            log.info("no git source on job {} - {}", job.getFlowName(), job.getBuildNumber());
            return;
        }

        try {
            var handler = getHandler(GitSource.valueOf(gitSourceStr));

            var commit = new GitCommitStatus();
            commit.setId(JobContextHelper.getCommitId(job));
            commit.setUrl(JobContextHelper.getGitUrl(job));
            commit.setTargetUrl(JobContextHelper.getJobUrl(job));
            commit.setStatus(JobContextHelper.getStatus(job).name().toLowerCase());
            commit.setDesc(String.format("build %s from flow.ci", commit.getStatus()));

            handler.writeCommit(commit);
        } catch (Throwable e) {
            log.warn(e.getMessage());
        }
    }

    private OperationHandler getHandler(GitSource source) {
        OperationHandler handler = handlers.get(source);
        if (Objects.isNull(handler)) {
            throw new UnsupportedException("Unsupported git source");
        }
        return handler;
    }

    private interface OperationHandler {

        GitConfig save(GitConfig config);

        void writeCommit(GitCommitStatus commit);
    }

    private class GitHubOperationHandler implements OperationHandler {

        private final GitAPIClient client = new GithubAPIClient(httpClient, objectMapper);

        @Override
        public GitConfig save(GitConfig config) {
            var event = eventManager.publish(new GetSecretEvent(this, config.getSecret()));
            Secret secret = event.getFetched();

            if (!(secret instanceof TokenSecret)) {
                throw new ArgumentException("Token secret is required");
            }

            var optional = gitConfigDao.findBySource(GitSource.GITHUB);
            if (optional.isEmpty()) {
                GitConfig c = new GitConfig(GitSource.GITHUB, secret.getName());
                return gitConfigDao.save(c);
            }

            GitConfig c = optional.get();
            c.setSecret(secret.getName());
            return gitConfigDao.save(c);
        }

        @Override
        public void writeCommit(GitCommitStatus commit) {
            var c = get(GitSource.GITHUB);
            var event = eventManager.publish(new GetSecretEvent(GitServiceImpl.this, c.getSecret()));
            Secret secret = event.getFetched();

            if (!(secret instanceof TokenSecret)) {
                throw new ArgumentException("Token secret is required");
            }

            TokenSecret tokenSecret = (TokenSecret) secret;
            client.writeCommitStatus(commit, tokenSecret);
        }
    }
}
