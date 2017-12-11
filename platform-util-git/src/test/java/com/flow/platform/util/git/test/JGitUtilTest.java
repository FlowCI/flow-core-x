/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.util.git.test;

import com.flow.platform.util.git.JGitUtil;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author yang
 */
public class JGitUtilTest {

    private static final String GIT_URL = "https://github.com/flow-ci-plugin/for-testing.git";

    private static final String ORIGIN = "origin";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void should_init_git_with_bare() throws Throwable {
        File gitDir = folder.newFolder("hello.git");
        JGitUtil.init(gitDir.toPath(), true);
        Assert.assertTrue(gitDir.exists());

        Repository repo = JGitUtil.getRepo(gitDir.toPath());
        Assert.assertNotNull(repo);
    }

    @Test
    public void should_git_clone_success() throws Throwable {
        File baseDir = folder.newFolder("info");
        Path path = JGitUtil.clone(GIT_URL, baseDir.toPath());
        Assert.assertEquals(true, path.toFile().exists());
    }

    @Test
    public void should_tags_success() throws Throwable {
        File localGitFolder = folder.newFolder("info.git");

        File onlineGitFolder = folder.newFolder("info");
        JGitUtil.clone(GIT_URL, onlineGitFolder.toPath());
        JGitUtil.init(localGitFolder.toPath(), true);

        // when get tags should get latest tag
        Assert.assertEquals(true, JGitUtil.tags(Git.open(localGitFolder).getRepository()).isEmpty());
        Assert.assertEquals("v1.1", JGitUtil.tags(onlineGitFolder.toPath()).get(0));
    }

    @Test
    public void should_push_success() throws Throwable {
        File localGitFolder = folder.newFolder("info.git");
        // when: init bareGit
        JGitUtil.init(localGitFolder.toPath(), true);

        // then: tag list is 0
        Assert.assertEquals(0, Git.open(localGitFolder).tagList().call().size());

        File onlineGitFolder = folder.newFolder("info");
        JGitUtil.clone(GIT_URL, onlineGitFolder.toPath());
        JGitUtil.remoteSet(onlineGitFolder.toPath(), "local", localGitFolder.toString());
        Git git = Git.open(onlineGitFolder);
        String tag = JGitUtil.tags(git.getRepository()).get(0);

        // when: push latest tag
        JGitUtil.push(onlineGitFolder.toPath(), "local", tag);

        // then: tag size is 1
        Assert.assertEquals(1, Git.open(localGitFolder).tagList().call().size());

        // then: tag is v1.1
        Assert.assertEquals("v1.1", JGitUtil.tags(localGitFolder.toPath()).get(0));
    }

    @Test
    public void should_set_remote_local_success() throws Throwable {
        File localGitFolder = folder.newFolder("info.git");

        File onlineGitFolder = folder.newFolder("info");
        JGitUtil.clone(GIT_URL, onlineGitFolder.toPath());
        JGitUtil.remoteSet(onlineGitFolder.toPath(), "local", localGitFolder.toString());
        Git git = Git.open(onlineGitFolder);

        Assert.assertEquals(2, git.remoteList().call().size());
    }

    @Test
    public void should_fetch_tags_success() throws Throwable {
        File localGitFolder = folder.newFolder("info.git");
        File onlineGitFolder = folder.newFolder("info");
        JGitUtil.init(onlineGitFolder.toPath(), false);
        JGitUtil.init(localGitFolder.toPath(), true);
        JGitUtil.remoteSet(onlineGitFolder.toPath(), "origin", GIT_URL);
        JGitUtil.remoteSet(onlineGitFolder.toPath(), "local", localGitFolder.toString());
        JGitUtil.fetchTags(onlineGitFolder.toPath(), GIT_URL);
        Git git = Git.open(onlineGitFolder);
        Assert.assertEquals(2, git.remoteList().call().size());
        Assert.assertEquals(false, JGitUtil.tags(git.getRepository()).isEmpty());
    }

    @Test
    public void should_list_commit_success() throws Throwable {

        File folder = this.folder.newFolder("info");
        JGitUtil.clone(GIT_URL, folder.toPath());
        JGitUtil.fetchTags(folder.toPath(), ORIGIN);

        JGitUtil.checkout(folder.toPath(), "v1.1");

        // when: list commit
        List<RevCommit> commits = JGitUtil.listCommits(folder.toPath());

        // then: should equal commit id
        Assert.assertEquals(34, commits.size());

        // then: should equal latest commit id
        Assert.assertEquals("30b728df5bdd3c51f8e032e90542d5d19eecf0f3",
            JGitUtil.listCommits(folder.toPath()).get(0).getId().getName());
    }

    @Test
    public void should_get_latest_commitid_success() throws Throwable {
        File folder = this.folder.newFolder("info");
        JGitUtil.clone(GIT_URL, folder.toPath());
        JGitUtil.fetchTags(folder.toPath(), ORIGIN);

        JGitUtil.checkout(folder.toPath(), "v1.0");
        Assert.assertEquals("ebc23db37df07069db39ad728288f5ef252298b8",
            JGitUtil.latestCommit(folder.toPath()).getId().getName());

    }
}
