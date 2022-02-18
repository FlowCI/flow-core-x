package com.flowci.core.git.client;

import com.flowci.core.git.domain.GitCommitWithJobStatus;

public interface GitClient {

    void writeCommitStatus(GitCommitWithJobStatus commit);
}
