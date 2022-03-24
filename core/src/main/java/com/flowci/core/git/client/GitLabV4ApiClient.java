package com.flowci.core.git.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.git.domain.GitCommitStatus;
import com.flowci.core.git.domain.GitConfigWithHost;
import com.flowci.core.secret.domain.AuthSecret;
import com.flowci.core.secret.domain.TokenSecret;
import com.flowci.exception.ArgumentException;
import com.flowci.util.StringHelper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Log4j2
public class GitLabV4ApiClient implements GitApiClient<GitConfigWithHost> {

    private final HttpClient httpClient;

    public GitLabV4ApiClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public void writeCommitStatus(GitCommitStatus commit, GitConfigWithHost config) {
        if (!StringHelper.hasValue(commit.getRepoId())) {
            throw new ArgumentException("GitLab repo id is missing");
        }

        var url = UriComponentsBuilder.fromHttpUrl(config.getHost())
                .path("/api/v4/projects/")
                .path(commit.getRepoId())
                .path("/statuses/")
                .path(commit.getId())
                .build()
                .toUriString();

        var request = getRequestBuilder(url, (TokenSecret) config.getSecretObj())
                .POST(buildFormData(ImmutableMap.builder()
                        .put("state", commit.getStatus())
                        .put("ref", commit.getBranch())
                        .put("context", "flow.ci")
                        .put("target_url", commit.getTargetUrl())
                        .put("description", commit.getDesc())
                        .build()))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(r -> {
                    log.info("Update commit status on {} is {}", commit.getUrl(), r.statusCode());
                    if (r.statusCode() != HttpStatus.OK.value()) {
                        log.debug(r.body());
                    }
                });
    }

    private HttpRequest.Builder getRequestBuilder(String url, TokenSecret secret) {
        return HttpRequest.newBuilder(URI.create(url))
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secret.getTokenData());
    }

    public static HttpRequest.BodyPublisher buildFormData(Map<Object, Object> data) {
        var builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }
}
