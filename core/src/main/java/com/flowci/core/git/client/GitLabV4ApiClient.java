package com.flowci.core.git.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.git.domain.GitCommitStatus;
import com.flowci.core.git.domain.GitConfigWithHost;
import com.flowci.core.secret.domain.TokenSecret;
import com.flowci.exception.ArgumentException;
import lombok.extern.log4j.Log4j2;

import java.net.http.HttpClient;

@Log4j2
public class GitLabV4ApiClient implements GitApiClient<GitConfigWithHost> {

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    public GitLabV4ApiClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void writeCommitStatus(GitCommitStatus commit, GitConfigWithHost config) {
        var secret = config.getSecretObj();
        if (!(secret instanceof TokenSecret)) {
            throw new ArgumentException("TokenSecret is required for Gerrit");
        }


    }
}
