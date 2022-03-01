package com.flowci.core.test.git;

import com.flowci.core.git.client.GithubAPIClient;
import com.flowci.core.git.domain.GitCommit;
import com.flowci.core.git.domain.GitRepo;
import org.junit.Test;
import org.testng.Assert;

public class GitHubClientTest {

    @Test
    public void should_extract_owner_repo_from_commit_url() {
        GitCommit commit = new GitCommit();
        commit.setUrl("https://github.com/gy2006/ci-test.git");

        GitRepo repo = GithubAPIClient.getRepo(commit);
        Assert.assertEquals("gy2006", repo.getOwner());
        Assert.assertEquals("ci-test", repo.getName());

        commit.setUrl("git@github.com:gy2006/ci-test.git");
        repo = GithubAPIClient.getRepo(commit);
        Assert.assertEquals("gy2006", repo.getOwner());
        Assert.assertEquals("ci-test", repo.getName());

        commit.setUrl("ssh://git@github.com:gy2006/ci-test.git");
        repo = GithubAPIClient.getRepo(commit);
        Assert.assertEquals("gy2006", repo.getOwner());
        Assert.assertEquals("ci-test", repo.getName());
    }
}
