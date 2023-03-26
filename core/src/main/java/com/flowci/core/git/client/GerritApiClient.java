package com.flowci.core.git.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.git.domain.GitCommitStatus;
import com.flowci.core.git.domain.GitConfigWithHost;
import com.flowci.core.git.util.CommitHelper;
import com.flowci.core.secret.domain.AuthSecret;
import com.flowci.exception.CIException;
import com.flowci.exception.NotFoundException;
import com.flowci.util.StringHelper;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
public class GerritApiClient implements GitApiClient<GitConfigWithHost> {

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    public GerritApiClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Write commit status on change only
     */
    @Override
    public void writeCommitStatus(GitCommitStatus commit, GitConfigWithHost config) {
        var secret = config.getSecretObj();
        var changeId = CommitHelper.getChangeId(commit);
        if (changeId.isEmpty()) {
            throw new NotFoundException("Change-Id not found from commit message");
        }

        try {
            var url = UriComponentsBuilder.fromHttpUrl(config.getHost())
                    .path("/a/changes/")
                    .path(changeId.get())
                    .path("/revisions/")
                    .path(commit.getId())
                    .path("/review")
                    .build()
                    .toUriString();

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
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setHeader(HttpHeaders.AUTHORIZATION, "Basic " + StringHelper.toBase64(str));
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
