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

import com.flow.platform.util.ExceptionUtil;
import com.flow.platform.util.git.model.GitCommit;
import com.flow.platform.util.git.model.GitProject;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.FileUtils;

/**
 * @author yang
 */
public abstract class JGitBasedClient implements GitClient {

    protected static final int GIT_TRANS_TIMEOUT = 30; // in seconds

    /**
     * The url of git repo
     */
    protected String gitUrl;

    /**
     * The dir of .git file, ex: /targetDir/.git
     */
    protected Path targetDir; // target base directory

    public JGitBasedClient(String gitUrl, Path baseDir) {
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
    public File clone(String branch, boolean noCheckout) throws GitException {
        checkGitUrl();

        CloneCommand cloneCommand = Git.cloneRepository()
            .setURI(gitUrl)
            .setNoCheckout(noCheckout)
            .setBranch(branch)
            .setDirectory(targetDir.toFile());

        try (Git git = buildCommand(cloneCommand).call()) {
            return git.getRepository().getDirectory();
        } catch (GitAPIException e) {
            throw new GitException("Fail to clone git repo", e);
        }
    }

    @Override
    public File clone(String branch, Set<String> checkoutFiles, ProgressMonitor monitor) throws GitException {
        checkGitUrl();

        File gitDir = getGitPath().toFile();

        // delete existing git folder since may have conflict
        if (gitDir.exists()) {
            try {
                FileUtils.delete(gitDir.getParentFile(), FileUtils.RECURSIVE);
            } catch (IOException e) {
                // IO error on delete existing folder
            }
        }

        // git init
        initGit(checkoutFiles);

        pull(branch, monitor);
        return gitDir;
    }

    @Override
    public String fetch(String branch, String filePath, ProgressMonitor monitor) throws GitException {
        clone(branch, Sets.newHashSet(filePath), monitor);
        Path targetPath = Paths.get(targetDir.toString(), filePath);

        if (Files.exists(targetPath)) {
            try {
                return com.google.common.io.Files.toString(targetPath.toFile(), Charset.forName("UTF-8"));
            } catch (IOException e) {
                return null;
            }
        }

        return null;
    }

    @Override
    public void pull(String branch, ProgressMonitor monitor) throws GitException {
        try (Git git = gitOpen()) {
            PullCommand pullCommand = pullCommand(branch, git);
            if (monitor != null) {
                pullCommand.setProgressMonitor(monitor);
            } else {
                pullCommand.setProgressMonitor(new DebugProgressMonitor());
            }
            pullCommand.call();
        } catch (Throwable e) {
            throw new GitException("Fail to pull with specific files: " + ExceptionUtil.findRootCause(e).getMessage());
        }
    }

    @Override
    public List<GitProject> projects() throws GitException {
        throw new UnsupportedOperationException("Unable to list git projects for JGit based client");
    }

    @Override
    public void setCurrentProject(GitProject project) throws GitException {
        throw new UnsupportedOperationException("Unable to set git project for JGit based client");
    }

    @Override
    public List<String> branches() throws GitException {
        try {
            Collection<Ref> refs = buildCommand(Git.lsRemoteRepository()
                .setHeads(true)
                .setTimeout(GIT_TRANS_TIMEOUT)
                .setRemote(gitUrl)).call();

            return toRefString(refs);
        } catch (GitAPIException e) {
            throw new GitException("Fail to list branches from remote repo", e);
        }
    }

    @Override
    public List<String> tags() throws GitException {
        try {
            Collection<Ref> refs = buildCommand(Git.lsRemoteRepository()
                .setTags(true)
                .setTimeout(GIT_TRANS_TIMEOUT)
                .setRemote(gitUrl)).call();

            return toRefString(refs);
        } catch (GitAPIException e) {
            throw new GitException("Fail to list tags from remote repo", ExceptionUtil.findRootCause(e));
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

            if (head == null) {
                return null;
            }

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

    @Override
    public String toString() {
        return "GitClient{" +
            "gitUrl='" + gitUrl + '\'' +
            ", targetDir=" + targetDir +
            ", clientType=" + getClass().getSimpleName() +
            '}';
    }

    /**
     * Git command builder
     */
    protected abstract <T extends TransportCommand> T buildCommand(T command);

    private List<String> toRefString(Collection<Ref> refs) {
        List<String> refStringList = new ArrayList<>(refs.size());

        for (Ref ref : refs) {
            // convert ref name from ref/head/master to master
            String refName = ref.getName();
            String simpleName = refName.replaceFirst("refs/heads/", "");

            // add to result list
            refStringList.add(simpleName);
        }

        return refStringList;
    }

    /**
     * Create local .git with remote info
     *
     * @return .git file path, /targetDir/.git
     */
    private File initGit(Set<String> checkoutFiles) throws GitException {
        try (Git git = Git.init().setDirectory(targetDir.toFile()).call()) {
            Repository repository = git.getRepository();
            File gitDir = repository.getDirectory();
            setSparseCheckout(gitDir, checkoutFiles);
            configRemote(repository.getConfig(), "origin", gitUrl);
            return gitDir;
        } catch (GitAPIException e) {
            throw new GitException("Fail to init git repo at: " + targetDir, e);
        }
    }

    private Git gitOpen() throws GitException {
        try {
            return Git.open(getGitPath().toFile());
        } catch (IOException e) {
            throw new GitException("Fail to open .git folder", e);
        }
    }

    private Path getGitPath() {
        return Paths.get(targetDir.toString(), ".git");
    }

    private void checkGitUrl() {
        if (Strings.isNullOrEmpty(gitUrl)) {
            throw new IllegalStateException("Please provides git url");
        }
    }

    private PullCommand pullCommand(String branch, Git git) {
        if (Strings.isNullOrEmpty(branch)) {
            return buildCommand(git.pull());
        }
        return buildCommand(git.pull().setRemoteBranchName(branch)).setTimeout(GIT_TRANS_TIMEOUT);
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

    private void setSparseCheckout(File gitDir, Set<String> checkoutFiles) throws GitException {
        try (Git git = gitOpen()) {
            StoredConfig config = git.getRepository().getConfig();
            config.setBoolean("core", null, "sparseCheckout", true);
            config.save();

            Path sparseCheckoutPath = Paths.get(gitDir.getAbsolutePath(), "info", "sparse-checkout");
            try {
                Files.createDirectory(sparseCheckoutPath.getParent());
                Files.createFile(sparseCheckoutPath);
            } catch (FileAlreadyExistsException ignore) {
            }

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
