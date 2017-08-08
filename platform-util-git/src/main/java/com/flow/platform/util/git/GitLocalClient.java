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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * @author yang
 */
public class GitLocalClient implements GitClient {

    protected String gitUrl;

    protected Path targetDir; // target directory

    public GitLocalClient(String gitUrl, Path targetDir) {
        this.gitUrl = gitUrl;
        this.targetDir = targetDir;
    }

    @Override
    public void pull(Integer depth) {
        // exec git linux command since jGit doesn't support sparse checkout
        try (Git git = gitOpen()) {
            String branch = git.getRepository().getBranch();

            String pullCmd = pullShellCommand(depth, "origin", branch).toString();
            ProcessBuilder pBuilder = new ProcessBuilder("/bin/bash", "-c", pullCmd);
            pBuilder.directory(targetDir.toFile());

            Process exec = pBuilder.start();
            exec.waitFor();
        } catch (Throwable e) {
            throw new GitException("Fail to pull with specific files", e);
        }
    }

    @Override
    public Collection<Ref> branches() {
        try (Git git = gitOpen()) {
            return git.branchList().call();
        } catch (GitAPIException e) {
            throw new GitException("Fail to list branches", e);
        }
    }

    @Override
    public Collection<Ref> tags() {
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
    public GitCommit commit(String refName) {
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

    protected StringBuilder pullShellCommand(Integer depth, String remote, String branch) {
        StringBuilder cmdBuilder = new StringBuilder("git pull");

        if (depth != null) {
            cmdBuilder.append(" --depth ").append(depth);
        }

        cmdBuilder.append(" ").append(remote);
        cmdBuilder.append(" ").append(branch);

        return cmdBuilder;
    }

    protected Git gitOpen() {
        try {
            return Git.open(Paths.get(targetDir.toString(), ".git").toFile());
        } catch (IOException e) {
            throw new GitException("Fail to open .git folder", e);
        }
    }
}
