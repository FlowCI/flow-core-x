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

package com.flow.platform.api.test.service;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.envs.GitEnvs;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.service.GitService;
import com.flow.platform.api.service.GitService.ProgressListener;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.util.git.model.GitCommit;
import com.flow.platform.util.git.model.GitSource;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileSystemUtils;

/**
 * @author yang
 */
public class GitServiceTest extends TestBase {

    @Autowired
    private GitService gitService;

    @Autowired
    private Path workspace;

    @Autowired
    private Path gitWorkspace;

    private Node node;

    @Before
    public void initNodeWithGitInfo() throws Throwable {
        node = new Node("flow_test", "flow_test");
        node.putEnv(GitEnvs.FLOW_GIT_SOURCE, GitSource.UNDEFINED_SSH.name());
        node.putEnv(GitEnvs.FLOW_GIT_URL, TestBase.GITHUB_TEST_REPO_SSH);
        node.putEnv(GitEnvs.FLOW_GIT_SSH_PRIVATE_KEY, getResourceContent("ssh_private_key"));
        node.putEnv(GitEnvs.FLOW_GIT_BRANCH, "master");
    }

    @Test
    public void should_list_existing_repo_from_git_workspace() throws Throwable {
        // given: copy exit git to workspace
        ClassLoader classLoader = TestBase.class.getClassLoader();
        URL resource = classLoader.getResource("hello.git");
        File path = new File(resource.getFile());
        FileUtils.copyDirectoryToDirectory(path, gitWorkspace.toFile());

        // when: load repos from git workspace
        List<Repository> repos = gitService.repos();
        Assert.assertEquals(1, repos.size());

        // then:
        Repository helloRepo = repos.get(0);
        Map<String, Ref> tags = helloRepo.getTags();
        Assert.assertEquals(1, tags.size());
        Assert.assertTrue(tags.keySet().contains("v1.0"));
    }

    @Test
    public void should_clone_git_file_with_ssh_pk() throws Throwable {
        String content = gitService.fetch(node, AppConfig.DEFAULT_YML_FILE, new ProgressListener() {

            @Override
            public void onStart() {
                System.out.println("Start fetch");
            }

            @Override
            public void onStartTask(String task) {
                // it means git url connected
                System.out.println("Start task: " + task);
            }

            @Override
            public void onProgressing(String task, int total, int progress) {
                System.out.println("Task: " + task + " : " + total + " : " + progress);
            }

            @Override
            public void onFinishTask(String task) {
                System.out.println("Task finished: " + task);
            }
        });

        Assert.assertNotNull(content);

        // get latest commit from local git repo
        GitCommit gitCommit = gitService.latestCommit(node);
        Assert.assertNotNull(gitCommit);
        Assert.assertNotNull(gitCommit.getMessage());
        Assert.assertNotNull(gitCommit.getAuthor());
        Assert.assertNotNull(gitCommit.getId());
    }

    @Test
    public void should_list_branches_of_git_repo() {
        List<String> branches = gitService.branches(node, false);
        Assert.assertNotNull(branches);
        Assert.assertEquals("develop", branches.get(0));
        Assert.assertEquals("master", branches.get(1));

        // should load branches from cache
        branches = gitService.branches(node, false);
        Assert.assertNotNull(branches);

        // should load branched from git repo for refresh
        branches = gitService.branches(node, true);
        Assert.assertNotNull(branches);
    }

    @Test
    public void should_list_tags_of_git_repo() {
        List<String> tags = gitService.tags(node, false);
        Assert.assertNotNull(tags);
    }

    @After
    public void after() throws Throwable {
        FileSystemUtils.deleteRecursively(workspace.toFile());
    }
}
