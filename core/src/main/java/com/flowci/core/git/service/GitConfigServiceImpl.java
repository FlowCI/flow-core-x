package com.flowci.core.git.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.common.exception.ArgumentException;
import com.flowci.common.exception.NotFoundException;
import com.flowci.common.exception.UnsupportedException;
import com.flowci.common.helper.StringHelper;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.git.client.GerritApiClient;
import com.flowci.core.git.client.GitApiClient;
import com.flowci.core.git.client.GitHubApiClient;
import com.flowci.core.git.client.GitLabV4ApiClient;
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
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.util.*;

@Slf4j
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
        handlers.put(GitSource.GITLAB, new GitLabOperationHandler());
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
        if (StringHelper.isEmpty(gitSourceStr)) {
            log.info("no git source on job {} - {}", job.getFlowName(), job.getBuildNumber());
            return;
        }

        try {
            GitSource source = GitSource.valueOf(gitSourceStr);
            var handler = getHandler(source);

            var commit = new GitCommitStatus();
            commit.setId(JobContextHelper.getCommitId(job));
            commit.setRepoId(JobContextHelper.getRepoId(job));
            commit.setBranch(JobContextHelper.getGitBranch(job));
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
        GitConfig save(GitConfig config) {
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
        void writeCommit(GitCommitStatus commit, GitConfig config) {
            Secret secret = fetch(config.getSecret(), TokenSecret.class);
            config.setSecretObj(secret);
            client.writeCommitStatus(commit, config);
        }
    }

    private class GerritOperationHandler extends OperationHandler {

        private final GitApiClient<GitConfigWithHost> client = new GerritApiClient(httpClient, objectMapper);

        @Override
        GitConfig save(GitConfig config) {
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
        void writeCommit(GitCommitStatus commit, GitConfig config) {
            Secret secret = fetch(config.getSecret(), AuthSecret.class);
            config.setSecretObj(secret);
            client.writeCommitStatus(commit, castConfig(config));
        }
    }

    private class GitLabOperationHandler extends OperationHandler {

        private final GitApiClient<GitConfigWithHost> client = new GitLabV4ApiClient(httpClient);

        @Override
        GitConfig save(GitConfig config) {
            var c = castConfig(config);
            Secret secret = fetch(config.getSecret(), TokenSecret.class);

            var optional = gitConfigDao.findBySource(GitSource.GITLAB);
            if (optional.isEmpty()) {
                return gitConfigDao.save(c);
            }

            var exist = (GitConfigWithHost) optional.get();
            exist.setSecret(secret.getName());
            exist.setHost(c.getHost());
            return gitConfigDao.save(exist);
        }

        @Override
        void writeCommit(GitCommitStatus commit, GitConfig config) {
            Secret secret = fetch(config.getSecret(), TokenSecret.class);
            config.setSecretObj(secret);
            client.writeCommitStatus(commit, castConfig(config));
        }
    }

    private static GitConfigWithHost castConfig(GitConfig config) {
        if (!(config instanceof GitConfigWithHost)) {
            throw new ArgumentException("GitConfigWithHost is required");
        }

        return (GitConfigWithHost) config;
    }
}
