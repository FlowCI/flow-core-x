package com.flowci.core.git.client;

import com.flowci.core.git.domain.GitCommitStatus;

public interface GitClient {

    void writeCommitStatus(GitCommitStatus commit);
}
