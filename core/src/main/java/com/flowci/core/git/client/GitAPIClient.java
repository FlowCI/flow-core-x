package com.flowci.core.git.client;

import com.flowci.core.git.domain.GitCommitStatus;
import com.flowci.core.secret.domain.Secret;

public interface GitAPIClient {

    void writeCommitStatus(GitCommitStatus commit, Secret secret);
}
