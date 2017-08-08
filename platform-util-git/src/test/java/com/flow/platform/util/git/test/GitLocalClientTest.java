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

import com.flow.platform.util.git.model.GitCommit;
import com.flow.platform.util.git.GitLocalClient;
import com.flow.platform.util.git.GitSshClient;
import com.google.common.collect.Lists;
import java.nio.file.Path;
import java.util.Collection;
import org.eclipse.jgit.lib.Ref;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author yang
 */
public class GitLocalClientTest {

    private final static String TEST_GIT_SSH_URL = "git@github.com:flow-ci-plugin/for-testing.git";

    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    @Test
    public void should_load_commit_by_branch() throws Throwable {
        // init: clone from remote
        Path targetDir = folder.newFolder().toPath();
        GitSshClient sshClient = new GitSshClient(TEST_GIT_SSH_URL, null, targetDir);
        sshClient.clone(false);

        // when: load from local git
        GitLocalClient gitClient = new GitLocalClient(null, targetDir);
        Collection<Ref> branches = gitClient.branches();

        // then: verify commit info
        GitCommit commit = gitClient.commit(Lists.newArrayList(branches).get(0).getName());
        Assert.assertNotNull(commit.getId());
        Assert.assertNotNull(commit.getMessage());
        Assert.assertNotNull(commit.getAuthor());
    }

    @After
    public void after() {
        folder.delete();
    }
}
