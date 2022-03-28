package com.flowci.core.git.client;

import com.flowci.core.git.domain.GitCommitStatus;
import com.flowci.core.git.domain.GitConfig;

public interface GitApiClient<C extends GitConfig> {

    void writeCommitStatus(GitCommitStatus commit, C config);
}
