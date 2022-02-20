package com.flowci.core.git.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.git.dao.GitSettingsDao;
import com.flowci.core.git.domain.GitCommit;
import com.flowci.core.git.domain.GitCommitStatus;
import com.flowci.core.git.domain.GitRepo;
import com.flowci.core.git.domain.GitSettings;
import com.flowci.exception.CIException;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.UnsupportedException;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Objects;

@Log4j2
@Component
public class GithubClient implements GitClient {

    private final static UriTemplate HttpTemplate = new UriTemplate("https://github.com/{owner}/{repo}.git");

    private final static UriTemplate SshTemplate = new UriTemplate("git@github.com:{owner}/{repo}.git");

    private final static UriTemplate SshPrefixTemplate = new UriTemplate("ssh://git@github.com:{owner}/{repo}.git");

    private final static String CommitStatusAPI = "https://api.github.com/repos/%s/%s/statuses/%s";

    private final GitSettingsDao gitSettingsDao;

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    public GithubClient(GitSettingsDao gitSettingsDao, HttpClient httpClient, ObjectMapper objectMapper) {
        this.gitSettingsDao = gitSettingsDao;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void writeCommitStatus(GitCommitStatus commit) {
        GitRepo repo = getRepo(commit);

        var body = new GitCommitPostBody();
        body.state = commit.getStatus();
        body.targetUrl = commit.getTargetUrl();
        body.description = commit.getDesc();

        try {
            var api = String.format(CommitStatusAPI, repo.getOwner(), repo.getOwner(), commit.getId());
            var request = getRequestBuilder(api)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(r -> log.info("Update commit status on {} is {}", commit.getUrl(), r.statusCode()));

        } catch (JsonProcessingException e) {
            throw new CIException("Unable to convert github commit body to json: " + e.getMessage());
        }
    }

    private HttpRequest.Builder getRequestBuilder(String url) {
        var optional = gitSettingsDao.findByKey(GitSettings.Key);
        if (optional.isEmpty()) {
            throw new NotFoundException("Git settings is missing");
        }

        var tokenSecret = optional.get().getGitHubTokenSecret();
        Objects.requireNonNull(tokenSecret, "Github token secret is missing");

        return HttpRequest.newBuilder(URI.create(url))
                .setHeader("Accept", "application/vnd.github.v3+json")
                .setHeader("Authorization", "token " + tokenSecret);
    }

    private static class GitCommitPostBody {

        public String state;

        @JsonProperty("target_url")
        public String targetUrl;

        public String description;
    }

    public static GitRepo getRepo(GitCommit commit) {
        String url = commit.getUrl();
        Objects.requireNonNull(url, "commit url is required");

        if (HttpTemplate.matches(url)) {
            Map<String, String> match = HttpTemplate.match(url);
            return new GitRepo(match.get("owner"), match.get("repo"));
        }

        if (SshTemplate.matches(url)) {
            Map<String, String> match = SshTemplate.match(url);
            return new GitRepo(match.get("owner"), match.get("repo"));
        }

        if (SshPrefixTemplate.matches(url)) {
            Map<String, String> match = SshPrefixTemplate.match(url);
            return new GitRepo(match.get("owner"), match.get("repo"));
        }

        throw new UnsupportedException("Unable to get owner and repo from GitHub commit url");
    }
}
