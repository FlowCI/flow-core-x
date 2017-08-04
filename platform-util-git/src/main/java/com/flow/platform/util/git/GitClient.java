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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;

/**
 * @author yang
 */
public abstract class GitClient {

    private static final int GIT_TRANS_TIMEOUT = 30; // in seconds

    protected String gitUrl;

    protected GitClient(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    /**
     * Git clone from remote url
     *
     * @param destDir dest folder include .git
     * @param noCheckout git clone xxxx --no-checkout, only clone .git dir
     * @return .git folder path
     */
    public File clone(String destDir, boolean noCheckout) {
        try (Git git = cloneCommand(destDir, noCheckout).call()) {
            return git.getRepository().getDirectory();
        } catch (GitAPIException e) {
            throw new GitException("Fail to clone git repo", e);
        }
    }

    /**
     * Git clone from remote url with specific checkout files
     *
     * @param destDir dest folder include .git
     * @param checkoutFiles specific checkout file
     * @return .git folder path
     */
    public File clone(String destDir, int depth, Set<String> checkoutFiles) {
        File gitDir;
        try (Git git = Git.init().setDirectory(new File(destDir)).call()) {
            Repository repository = git.getRepository();

            gitDir = repository.getDirectory();
            setSparseCheckout(gitDir, checkoutFiles);
            configRemote(repository.getConfig(), "origin", gitUrl);

        } catch (GitAPIException e) {
            throw new GitException("Fail to clone git repo", e);
        } catch (GitException e) {
            throw e;
        }

        pull(destDir, depth);
        return gitDir;
    }

    public void pull(String destDir, int depth) {
        File gitDir = Paths.get(destDir, ".git").toFile();

        // exec git linux command since jGit doesn't support sparse checkout
        try (Git git = Git.open(gitDir)) {
            String branch = git.getRepository().getBranch();

            String pullCmd = pullShellCommand(depth, "origin", branch).toString();
            ProcessBuilder pBuilder = new ProcessBuilder("/bin/bash", "-c", pullCmd);
            pBuilder.directory(new File(destDir));

            Process exec = pBuilder.start();
            exec.waitFor();
        } catch (Throwable e) {
            throw new GitException("Fail to pull with specific files", e);
        }
    }

    public Collection<Ref> branches() {
        try {
            return listBranchCommand().call();
        } catch (GitAPIException e) {
            throw new GitException("Fail to list branch", e);
        }
    }

    public Collection<Ref> tags() {
        try {
            return listTagCommand().call();
        } catch (GitAPIException e) {
            throw new GitException("Fail to list tags", e);
        }
    }

    protected CloneCommand cloneCommand(String destDir, boolean noCheckout) {
        return Git.cloneRepository()
            .setURI(gitUrl)
            .setNoCheckout(noCheckout)
            .setDirectory(Paths.get(destDir).toFile());
    }

    protected LsRemoteCommand listBranchCommand() {
        return Git.lsRemoteRepository()
            .setHeads(true)
            .setTimeout(GIT_TRANS_TIMEOUT)
            .setRemote(gitUrl);
    }

    protected LsRemoteCommand listTagCommand() {
        return Git.lsRemoteRepository()
            .setTags(true)
            .setTimeout(GIT_TRANS_TIMEOUT)
            .setRemote(gitUrl);
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

    private void setSparseCheckout(File gitDir, Set<String> checkoutFiles) throws GitException {
        try (Git git = Git.open(gitDir)) {
            StoredConfig config = git.getRepository().getConfig();
            config.setBoolean("core", null, "sparseCheckout", true);
            config.save();

            Path sparseCheckoutPath = Paths.get(gitDir.getAbsolutePath(), "info", "sparse-checkout");
            Files.createDirectory(sparseCheckoutPath.getParent());
            Files.createFile(sparseCheckoutPath);
            Charset charset = Charset.forName("UTF-8");

            // load all existing
            Set<String> exists = new HashSet<>();
            try (BufferedReader reader = Files.newBufferedReader(sparseCheckoutPath, charset)) {
                exists.add(reader.readLine());
            }

            // write
            try (BufferedWriter writer = Files.newBufferedWriter(sparseCheckoutPath, charset)) {
                if (!exists.isEmpty()) {
                    writer.newLine();
                }

                for (String checkoutFile : checkoutFiles) {
                    if (exists.contains(checkoutFile)) {
                        continue;
                    }
                    writer.write(checkoutFile);
                    writer.newLine();
                }
            }

        } catch (Throwable e) {
            throw new GitException("Fail to set sparse checkout config", e);
        }
    }

    private void configRemote(StoredConfig config, String name, String url) throws GitException {
        try {
            config.setString("remote", name, "url", url);
            config.setString("remote", name, "fetch", "+refs/heads/*:refs/remotes/origin/*");
            config.save();
        } catch (IOException e) {
            throw new GitException("Fail to config remote git url", e);
        }
    }
}
