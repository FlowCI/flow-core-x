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

import static junit.framework.TestCase.fail;

import com.flow.platform.util.git.GitClient;
import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.GitLabClient;
import com.flow.platform.util.git.model.GitCommit;
import com.flow.platform.util.git.model.GitProject;
import com.google.common.base.Strings;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author yang
 */
public class GitLabClientTest {

    private GitClient client;

    @Before
    public void before() throws GitException {
        client = new GitLabClient("https://gitlab.com/", "E63AvvP5EvYhDwFySAE5", "yang.guo/for-testing");
        Assert.assertNotNull(client);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void should_throw_unsupported_operation_exception_for_clone() throws Throwable {
        client.clone("master", true);
    }

    @Test
    public void should_throw_git_exception_for_not_existed_project() throws Throwable {
        try {
            new GitLabClient("https://gitlab.com/", "E63AvvP5EvYhDwFySAE5", "yang.guo/hello");
            fail();
        } catch (GitException e) {
            Assert.assertTrue(e.getMessage().startsWith("Project not found"));
        }
    }

    @Test
    public void should_get_file_content_from_git() throws Throwable {
        String fileContent = client.fetch("master", ".flow.yml", null);
        Assert.assertEquals(false, Strings.isNullOrEmpty(fileContent));
    }

    @Test
    public void should_get_commit_info() throws Throwable {
        GitCommit commit = client.commit("master");
        Assert.assertNotNull(commit);
        Assert.assertNotNull(commit.getId());
        Assert.assertNotNull(commit.getAuthor());
        Assert.assertNotNull(commit.getMessage());
    }

    @Test
    public void should_list_all_projects() throws Throwable {
        List<GitProject> projects = client.projects();
        Assert.assertNotNull(projects);
        Assert.assertTrue(projects.size() >= 1);

        GitProject project = projects.get(0);
        Assert.assertNotNull(project.getId());
        Assert.assertNotNull(project.getName());
        Assert.assertNotNull(project.getFullName());
    }

    @Test
    public void should_list_branches() throws Throwable {
        List<String> branches = client.branches();
        Assert.assertTrue(branches.size() >= 1);
    }

    @Test
    public void should_list_tags() throws Throwable {
        List<String> tags = client.tags();
        Assert.assertTrue(tags.size() >= 1);
    }
}
