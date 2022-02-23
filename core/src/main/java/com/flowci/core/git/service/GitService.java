package com.flowci.core.git.service;

import com.flowci.core.git.domain.GitConfig;

public interface GitService {

    GitConfig saveGithubSecret(String secret);
}
