package com.flowci.core.test.git;

import com.flowci.core.git.client.GitHubApiClient;
import com.flowci.core.git.domain.GitCommit;
import com.flowci.core.git.domain.GitRepo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GitHubClientTest {

    @Test
    public void should_extract_owner_repo_from_commit_url() {
        GitCommit commit = new GitCommit();
        commit.setUrl("https://github.com/gy2006/ci-test.git");

        GitRepo repo = GitHubApiClient.getRepo(commit);
        assertEquals("gy2006", repo.getOwner());
        assertEquals("ci-test", repo.getName());

        commit.setUrl("git@github.com:gy2006/ci-test.git");
        repo = GitHubApiClient.getRepo(commit);
        assertEquals("gy2006", repo.getOwner());
        assertEquals("ci-test", repo.getName());

        commit.setUrl("ssh://git@github.com:gy2006/ci-test.git");
        repo = GitHubApiClient.getRepo(commit);
        assertEquals("gy2006", repo.getOwner());
        assertEquals("ci-test", repo.getName());
    }
}
