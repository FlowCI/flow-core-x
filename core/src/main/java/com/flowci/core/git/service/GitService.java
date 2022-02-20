package com.flowci.core.git.service;

import com.flowci.core.git.domain.GitCommitStatus;

public interface GitService {

    void writeStatus(GitCommitStatus commit);
}
