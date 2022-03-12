package com.flowci.core.git.client;

import com.flowci.core.git.domain.GitCommitStatus;
import com.flowci.core.git.domain.GitConfig;

public interface GitAPIClient {

    void writeCommitStatus(GitCommitStatus commit, GitConfig config);
}
