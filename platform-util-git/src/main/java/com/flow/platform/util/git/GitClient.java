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

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
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
     * @param destDir git dest dir
     * @param noCheckout git clone xxxx --no-checkout, only clone .git dir
     * @return .git folder
     */
    public File clone(String destDir, boolean noCheckout) {
        try (Git git = cloneCommand(destDir, noCheckout).call()) {
            return git.getRepository().getDirectory();
        } catch (GitAPIException e) {
            throw new GitException("Fail to clone git repo", e);
        }
    }

    public File clone(String destDir, Set<String> checkoutFiles) {
        File gitDir = clone(destDir, true);
        String branch;

        // setup sparse checkout
        try (Git git = Git.open(gitDir)) {
            StoredConfig config = git.getRepository().getConfig();
            configSparseCheckout(config, true);
            addSparseCheckoutFile(gitDir, checkoutFiles);
            branch = git.getRepository().getBranch();
        } catch (Throwable e) {
            throw new GitException("Fail to set sparse checkout config", e);
        }

        // exec git linux command since jGit doesn't support sparse checkout
        try {
            String pullCmd = "git pull origin " + branch;
            ProcessBuilder pBuilder = new ProcessBuilder("/bin/bash", "-c", pullCmd);
            pBuilder.directory(new File(destDir));

            Process exec = pBuilder.start();
            exec.waitFor();
        } catch (Throwable e) {
            throw new GitException("Fail to pull with specific files", e);
        }

        return gitDir;
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

    /**
     * @param gitPath .git folder path
     * @param files files to be added to sparse-checkout
     */
    private void addSparseCheckoutFile(File gitPath, Set<String> files) throws IOException {
        Path sparseCheckoutPath = Paths.get(gitPath.getAbsolutePath(), "info", "sparse-checkout");
        Files.createParentDirs(sparseCheckoutPath.toFile());

        for (String checkoutFile : files) {
            Files.append(checkoutFile, sparseCheckoutPath.toFile(), Charset.forName("UTF-8"));
        }
    }

    private void configSparseCheckout(StoredConfig config, boolean value) throws IOException {
        config.setBoolean("core", null, "sparseCheckout", value);
        config.save();
    }

    private void configRemote(StoredConfig config, String name, String url) throws IOException {
        config.setString("remote", name, "url", url);
        config.setString("remote", name, "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.save();
    }
}
