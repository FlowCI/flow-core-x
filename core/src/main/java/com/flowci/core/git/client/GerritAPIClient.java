package com.flowci.core.git.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.git.domain.GitCommitStatus;
import com.flowci.core.git.domain.GitConfig;
import com.flowci.core.git.domain.GitConfigWithHost;
import com.flowci.core.git.util.CommitHelper;
import com.flowci.core.secret.domain.AuthSecret;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.CIException;
import com.flowci.exception.NotFoundException;
import com.flowci.util.StringHelper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Log4j2
public class GerritAPIClient implements GitAPIClient {

    private final static String SetReviewAPI = "%s/repos/a/changes/%s/revisions/%s/review";

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    public GerritAPIClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Write commit status on change only
     */
    @Override
    public void writeCommitStatus(GitCommitStatus commit, GitConfig config) {
        if (!(config instanceof GitConfigWithHost)) {
            throw new ArgumentException("GitConfigWithHost is required for Gerrit");
        }

        var secret = config.getSecretObj();
        if (!(secret instanceof AuthSecret)) {
            throw new ArgumentException("AuthSecret is required for Gerrit");
        }

        var changeId = CommitHelper.getChangeId(commit);
        if (changeId.isEmpty()) {
            throw new NotFoundException("Change-Id not found from commit message");
        }

        try {
            var gitConfig = (GitConfigWithHost) config;
            var url = String.format(SetReviewAPI, gitConfig.getHost(), changeId.get(), commit.getId());
            var request = getRequestBuilder(url, (AuthSecret) secret)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(getBodyObj(commit))))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(r -> {
                        log.info("Update commit status on {} is {}", commit.getUrl(), r.statusCode());
                        if (r.statusCode() != HttpStatus.OK.value()) {
                            log.debug(r.body());
                        }
                    });

        } catch (JsonProcessingException e) {
            throw new CIException("Unable to convert gerrit commit body to json: " + e.getMessage());
        }
    }

    private HttpRequest.Builder getRequestBuilder(String url, AuthSecret secret) {
        var str = secret.getUsername() + ":" + secret.getPassword();
        return HttpRequest.newBuilder(URI.create(url))
                .setHeader("Authorization", "Basic " + StringHelper.toBase64(str));
    }

    private SetReviewBody getBodyObj(GitCommitStatus c) {
        var obj = new SetReviewBody();
        obj.tag = "flow.ci";
        obj.message = c.getDesc() + " - " + c.getTargetUrl();
        obj.labels = new Labels(-1);

        if (c.getStatus().contains("success")) {
            obj.labels.codeReview = 1;
        }

        return obj;
    }

    private static class SetReviewBody {

        public String tag;

        public String message;

        public Labels labels;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    private static class Labels {

        @JsonProperty("Code-Review")
        public int codeReview;
    }
}
