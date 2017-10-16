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

package com.flow.platform.util.git;

import com.flow.platform.util.git.model.GitCommit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabCommit;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabRepositoryFile;
import org.gitlab.api.models.GitlabTag;

/**
 * @author yang
 */
public class GitLabClient implements GitClient {

    private final String host;

    private final String token;

    private GitlabProject project; // NAMESPACE/PROJECT_NAME

    private final GitlabAPI connect;

    public GitLabClient(String host, String token, String project) throws GitException {
        this.host = host;
        this.token = token;
        this.connect = GitlabAPI.connect(host, token);

        // init gitlab project if project name given
        if (project != null) {
            try {
                this.project = this.connect.getProject(project);
            } catch (IOException e) {
                if (e instanceof FileNotFoundException) {
                    throw new GitException("Project not found: " + e.getMessage());
                }
                throw new GitException(e.getMessage());
            }
        }
    }

    public String getHost() {
        return host;
    }

    public String getToken() {
        return token;
    }

    @Override
    public Path targetPath() {
        throw new UnsupportedOperationException("Target path not supported for GitLab");
    }

    @Override
    public File clone(String branch, boolean noCheckout) throws GitException {
        throw new UnsupportedOperationException("Git clone not supported for GitLab");
    }

    @Override
    public File clone(String branch, Set<String> checkoutFiles, ProgressMonitor monitor) throws GitException {
        throw new UnsupportedOperationException("Git clone not supported for GitLab");
    }

    @Override
    public String fetch(String branch, String filePath, ProgressMonitor monitor) throws GitException {
        try {
            GitlabRepositoryFile file = connect.getRepositoryFile(project, filePath, branch);
            String base64Content = file.getContent();
            byte[] contentBytes = Base64.getDecoder().decode(base64Content);
            return new String(contentBytes, "UTF-8");
        } catch (IOException e) {
            throw new GitException(e.getMessage());
        }
    }

    @Override
    public void pull(String branch, ProgressMonitor monitor) throws GitException {
        throw new UnsupportedOperationException("Pull not supported for GitLab");
    }

    @Override
    public List<String> branches() throws GitException {
        try {
            List<GitlabBranch> branches = connect.getBranches(project);
            return toStringBranches(branches);
        } catch (IOException e) {
            throw new GitException(e.getMessage());
        }
    }

    @Override
    public List<String> tags() throws GitException {
        try {
            List<GitlabTag> tags = connect.getTags(project);
            return toStringTags(tags);
        } catch (IOException e) {
            throw new GitException(e.getMessage());
        }
    }

    @Override
    public GitCommit commit(String refName) throws GitException {
        try {
            List<GitlabCommit> lastCommits = connect.getLastCommits(project.getId(), refName);
            GitlabCommit gitlabCommit = lastCommits.get(0);

            String commitId = gitlabCommit.getId();
            String commitMessage = gitlabCommit.getMessage();
            String commitAuthorEmail = gitlabCommit.getAuthorEmail();

            return new GitCommit(commitId, commitMessage, commitAuthorEmail);
        } catch (IOException e) {
            throw new GitException(e.getMessage());
        }
    }

    private List<String> toStringBranches(Collection<GitlabBranch> branches) {
        List<String> list = new ArrayList<>(branches.size());
        for (GitlabBranch branch : branches) {
            list.add(branch.getName());
        }
        return list;
    }

    private List<String> toStringTags(Collection<GitlabTag> tags) {
        List<String> list = new ArrayList<>(tags.size());
        for (GitlabTag tag : tags) {
            list.add(tag.getName());
        }
        return list;
    }
}
