package com.flowci.core.git.service;

import com.flowci.core.git.domain.GitSettings;

public interface GitService {

    GitSettings saveGithubSecret(String secret);
}
