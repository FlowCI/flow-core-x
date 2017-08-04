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

import com.flow.platform.util.git.GitClient;
import com.flow.platform.util.git.SshGitClient;
import com.google.common.collect.Sets;
import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import org.eclipse.jgit.lib.Ref;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author yang
 */
public class GitClientTest {

    private final static String TEST_GIT_SSH_URL = "git@github.com:flow-ci-plugin/for-testing.git";

    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    @Test
    public void should_load_all_branch_and_tags() throws Throwable {
        GitClient client = new SshGitClient(TEST_GIT_SSH_URL);

        // load all branches
        Collection<Ref> branches = client.branches();
        Assert.assertNotNull(branches);
        Assert.assertTrue(branches.size() >= 1);

        // load all tags
        Collection<Ref> tags = client.tags();
        Assert.assertNotNull(tags);
        Assert.assertTrue(tags.size() >= 1);
    }

    @Test
    public void should_clone_repo() throws Throwable {
        // when: clone only for plugin config file
        SshGitClient gitClient = new SshGitClient(TEST_GIT_SSH_URL);

        String tmpPath = folder.getRoot().getAbsolutePath();
        gitClient.clone(tmpPath, 1, Sets.newHashSet(".flow.yml"));

        final Set<String> acceptedFiles = Sets.newHashSet(".git", ".flow.yml");

        // then:
        File[] files = Paths.get(tmpPath).toFile().listFiles();
        Assert.assertEquals(2, files.length);
        for (File file : files) {
            Assert.assertTrue(acceptedFiles.contains(file.getName()));
        }
    }
}
