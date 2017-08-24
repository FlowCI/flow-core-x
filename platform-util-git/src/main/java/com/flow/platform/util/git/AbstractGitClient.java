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
import com.google.common.base.Strings;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * @author yang
 */
public abstract class AbstractGitClient implements GitClient {

    protected String gitUrl;

    protected Path targetDir; // target base directory

    public AbstractGitClient(String gitUrl, Path baseDir) {
        this.gitUrl = gitUrl;

        // verify git url
        int dotGitIndex = gitUrl.lastIndexOf(".git");
        if (dotGitIndex == -1) {
            throw new IllegalArgumentException("Illegal git url");
        }

        int lastSlashIndex = gitUrl.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            throw new IllegalArgumentException("Illegal git url");
        }

        if (lastSlashIndex >= dotGitIndex) {
            throw new IllegalArgumentException("Illegal git url");
        }

        String repoName = gitUrl.substring(lastSlashIndex + 1, dotGitIndex);
        this.targetDir = Paths.get(baseDir.toString(), repoName);
    }

    @Override
    public Path targetPath() {
        return this.targetDir;
    }

    @Override
    public void pull(String branch, Integer depth) throws GitException {
        if (depth != null) {
            throw new GitException("JGit api doesn't support shallow clone");
        }

        try (Git git = gitOpen()) {
            pullCommand(branch, git).setProgressMonitor(new DebugProgressMonitor()).call();
        } catch (Throwable e) {
            throw new GitException("Fail to pull with specific files", e);
        }
    }

    @Override
    public Collection<Ref> branches() throws GitException {
        try (Git git = gitOpen()) {
            return git.branchList().call();
        } catch (GitAPIException e) {
            throw new GitException("Fail to list branches", e);
        }
    }

    @Override
    public Collection<Ref> tags() throws GitException {
        try (Git git = gitOpen()) {
            return git.tagList().call();
        } catch (GitAPIException e) {
            throw new GitException("Fail to list tags", e);
        }
    }

    /**
     * Get latest commit by ref name from local .git
     */
    @Override
    public GitCommit commit(String refName) throws GitException {
        try (Git git = gitOpen()) {
            Repository repo = git.getRepository();
            Ref head = repo.findRef(refName);

            try (RevWalk walk = new RevWalk(repo)) {
                RevCommit commit = walk.parseCommit(head.getObjectId());
                walk.dispose();

                String id = commit.getId().getName();
                String message = commit.getFullMessage();
                String author = commit.getAuthorIdent().getName();

                return new GitCommit(id, message, author);
            }
        } catch (IOException e) {
            throw new GitException("Fail to get commit message", e);
        }
    }

    protected PullCommand pullCommand(String branch, Git git) {
        if (Strings.isNullOrEmpty(branch)) {
            return git.pull();
        }
        return git.pull().setRemoteBranchName(branch);
    }

    Git gitOpen() throws GitException {
        try {
            return Git.open(getGitPath().toFile());
        } catch (IOException e) {
            throw new GitException("Fail to open .git folder", e);
        }
    }

    Path getGitPath() {
        return Paths.get(targetDir.toString(), ".git");
    }

    @Override
    public String toString() {
        return "GitClient{" +
            "gitUrl='" + gitUrl + '\'' +
            ", targetDir=" + targetDir +
            ", clientType=" + getClass().getSimpleName() +
            '}';
    }

    private class DebugProgressMonitor implements ProgressMonitor {

        private String task;
        private int totalWork;

        @Override
        public void start(int totalTasks) {
            System.out.println("Git total task: " + totalTasks);
        }

        @Override
        public void beginTask(String title, int totalWork) {
            this.task = title;
            this.totalWork = totalWork;
            System.out.println("Git begin task: " + task + ", " + totalWork);
        }

        @Override
        public void update(int completed) {
            System.out.println("Git on task: " + task + ", " + completed);
        }

        @Override
        public void endTask() {
            System.out.println("Git end task: " + task);
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}
