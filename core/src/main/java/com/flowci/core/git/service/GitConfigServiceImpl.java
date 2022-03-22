package com.flowci.core.git.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.git.client.GerritApiClient;
import com.flowci.core.git.client.GitApiClient;
import com.flowci.core.git.client.GitHubApiClient;
import com.flowci.core.git.dao.GitConfigDao;
import com.flowci.core.git.domain.GitCommitStatus;
import com.flowci.core.git.domain.GitConfig;
import com.flowci.core.git.domain.GitConfigWithHost;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.JobFinishedEvent;
import com.flowci.core.job.util.JobContextHelper;
import com.flowci.core.secret.domain.AuthSecret;
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
@Service("gitConfigService")
public class GitConfigServiceImpl implements GitConfigService {

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
        handlers.put(GitSource.GERRIT, new GerritOperationHandler());
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
            GitSource source = GitSource.valueOf(gitSourceStr);
            var handler = getHandler(source);

            var commit = new GitCommitStatus();
            commit.setId(JobContextHelper.getCommitId(job));
            commit.setMessage(JobContextHelper.getGitMessage(job));
            commit.setUrl(JobContextHelper.getGitUrl(job));
            commit.setTargetUrl(JobContextHelper.getJobUrl(job));
            commit.setStatus(JobContextHelper.getStatus(job).name().toLowerCase());
            commit.setDesc(String.format("build %s from flow.ci", commit.getStatus()));

            handler.writeCommit(commit, get(source));
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

    private abstract class OperationHandler {

        abstract GitConfig save(GitConfig config);

        abstract void writeCommit(GitCommitStatus commit, GitConfig config);

        Secret fetch(String name, Class<?> expected) {
            var event = eventManager.publish(new GetSecretEvent(this, name));
            Secret secret = event.getFetched();

            if (!expected.isInstance(secret)) {
                throw new ArgumentException("Secret type is not matched");
            }

            return secret;
        }
    }

    private class GitHubOperationHandler extends OperationHandler {

        private final GitApiClient<GitConfig> client = new GitHubApiClient(httpClient, objectMapper);

        @Override
        public GitConfig save(GitConfig config) {
            Secret secret = fetch(config.getSecret(), TokenSecret.class);

            var optional = gitConfigDao.findBySource(GitSource.GITHUB);
            if (optional.isEmpty()) {
                return gitConfigDao.save(config);
            }

            GitConfig c = optional.get();
            c.setSecret(secret.getName());
            return gitConfigDao.save(c);
        }

        @Override
        public void writeCommit(GitCommitStatus commit, GitConfig config) {
            Secret secret = fetch(config.getSecret(), TokenSecret.class);
            config.setSecretObj(secret);
            client.writeCommitStatus(commit, config);
        }
    }

    private class GerritOperationHandler extends OperationHandler {

        private final GitApiClient<GitConfigWithHost> client = new GerritApiClient(httpClient, objectMapper);

        @Override
        public GitConfig save(GitConfig config) {
            var c = castConfig(config);
            Secret secret = fetch(config.getSecret(), AuthSecret.class);

            var optional = gitConfigDao.findBySource(GitSource.GERRIT);
            if (optional.isEmpty()) {
                return gitConfigDao.save(c);
            }

            var exist = (GitConfigWithHost) optional.get();
            exist.setSecret(secret.getName());
            exist.setHost(c.getHost());
            return gitConfigDao.save(exist);
        }

        @Override
        public void writeCommit(GitCommitStatus commit, GitConfig config) {
            Secret secret = fetch(config.getSecret(), AuthSecret.class);
            config.setSecretObj(secret);
            client.writeCommitStatus(commit, castConfig(config));
        }

        private GitConfigWithHost castConfig(GitConfig config) {
            if (!(config instanceof GitConfigWithHost)) {
                throw new ArgumentException("GitConfigWithHost is required");
            }

            return (GitConfigWithHost) config;
        }
    }
}
