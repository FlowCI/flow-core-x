package com.flowci.core.git.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.git.client.GitAPIClient;
import com.flowci.core.git.client.GithubAPIClient;
import com.flowci.core.git.dao.GitSettingsDao;
import com.flowci.core.git.domain.GitCommitStatus;
import com.flowci.core.git.domain.GitSettings;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.JobFinishedEvent;
import com.flowci.core.job.util.JobContextHelper;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.domain.TokenSecret;
import com.flowci.core.secret.event.GetSecretEvent;
import com.flowci.exception.ArgumentException;
import com.flowci.util.StringHelper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Service("gitService")
public class GitServiceImpl implements GitService {

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GitSettingsDao gitSettingsDao;

    @Autowired
    private SpringEventManager eventManager;

    private final Map<GitSource, ActionHandler> actions = new HashMap<>(5);

    @PostConstruct
    public void init() {
        actions.put(GitSource.GITHUB, new GitHubActionHandler());

        var optional = gitSettingsDao.findByKey(GitSettings.Key);
        if (optional.isEmpty()) {
            try {
                gitSettingsDao.save(new GitSettings());
            } catch (DuplicateKeyException ignore) {
                // ignore the duplicate git settings key
            }
        }
    }

    @Override
    public GitSettings saveGithubSecret(String secretName) {
        var event = eventManager.publish(new GetSecretEvent(this, secretName));
        var event1 = eventManager.publish(new GetSecretEvent(this, secretName));
        Secret secret = event1.getFetched();

        if (!(secret instanceof TokenSecret)) {
            throw new ArgumentException("Token secret is required");
        }

        var settings = loadGitSettings();
        settings.setGitHubTokenSecret(secretName);
        return gitSettingsDao.save(settings);
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
            var source = GitSource.valueOf(gitSourceStr);
            var handler = actions.get(source);

            if (handler == null) {
                return;
            }

            var commit = new GitCommitStatus();
            commit.setId(JobContextHelper.getCommitId(job));
            commit.setUrl(JobContextHelper.getGitUrl(job));
            commit.setTargetUrl(JobContextHelper.getJobUrl(job));
            commit.setStatus(JobContextHelper.getStatus(job).name().toLowerCase());
            commit.setDesc(String.format("build %s from flow.ci", commit.getStatus()));

            handler.write(commit);
        } catch (Throwable e) {
            log.warn(e.getMessage());
        }
    }

    private GitSettings loadGitSettings() {
        return gitSettingsDao.findByKey(GitSettings.Key).get();
    }

    private interface ActionHandler {
        void write(GitCommitStatus commit);
    }

    private class GitHubActionHandler implements ActionHandler {

        private final GitAPIClient client = new GithubAPIClient(httpClient, objectMapper);

        @Override
        public void write(GitCommitStatus commit) {
            var settings = loadGitSettings();
            String secretName = settings.getGitHubTokenSecret();

            var event = eventManager.publish(new GetSecretEvent(GitServiceImpl.this, secretName));
            Secret secret = event.getFetched();

            if (!(secret instanceof TokenSecret)) {
                throw new ArgumentException("Token secret is required");
            }

            TokenSecret tokenSecret = (TokenSecret) secret;
            client.writeCommitStatus(commit, tokenSecret);
        }
    }

}
