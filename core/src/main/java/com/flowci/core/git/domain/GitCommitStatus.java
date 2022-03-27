package com.flowci.core.git.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GitCommitStatus extends GitCommit {

    private String status; // lower case from job status

    private String targetUrl;

    private String desc;

    private String repoId;

    private String branch;
}
