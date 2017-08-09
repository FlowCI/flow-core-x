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

import com.google.common.base.Strings;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.util.FS;

/**
 * @author yang
 */
public class GitSshClient extends AbstractGitClient {

    private static final int GIT_TRANS_TIMEOUT = 30; // in seconds

    private Path privateKeyPath;

    private TransportConfigCallback transportConfigCallback;

    public GitSshClient(String sshGitUrl, Path targetDir) {
        super(sshGitUrl, targetDir);
        initSessionFactory();
    }

    public GitSshClient(String gitUrl, Path privateKeyPath, Path targetDir) {
        super(gitUrl, targetDir);
        this.privateKeyPath = privateKeyPath;
        initSessionFactory();
    }

    public void setPrivateKeyPath(Path privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
        initSessionFactory();
    }

    @Override
    public File clone(boolean noCheckout) {
        checkGitUrl();

        CloneCommand cloneCommand = Git.cloneRepository()
            .setURI(gitUrl)
            .setNoCheckout(noCheckout)
            .setDirectory(targetDir.toFile());

        try (Git git = buildSshCommand(cloneCommand).call()) {
            return git.getRepository().getDirectory();
        } catch (GitAPIException e) {
            throw new GitException("Fail to clone git repo", e);
        }
    }

    @Override
    public File clone(Integer depth, Set<String> checkoutFiles) {
        checkGitUrl();
        File gitDir = getGitPath().toFile();

        // open existing git folder to verification
        if (gitDir.exists()) {
            try (Git git = Git.open(gitDir)) {

            } catch (IOException e) {
                throw new GitException("Fail to open existing git repo at: " + gitDir, e);
            }
        }

        // git init
        else {
            try (Git git = Git.init().setDirectory(targetDir.toFile()).call()){
                Repository repository = git.getRepository();
                gitDir = repository.getDirectory();
                setSparseCheckout(gitDir, checkoutFiles);
                configRemote(repository.getConfig(), "origin", gitUrl);
            } catch (GitAPIException e) {
                throw new GitException("Fail to init git repo at: " + gitDir, e);
            }
        }

        pull(depth);

        return gitDir;
    }

    @Override
    public Collection<Ref> branches() {
        try {
            return buildSshCommand(Git.lsRemoteRepository()
                .setHeads(true)
                .setTimeout(GIT_TRANS_TIMEOUT)
                .setRemote(gitUrl)).call();
        } catch (GitAPIException e) {
            throw new GitException("Fail to list branches from remote repo", e);
        }
    }

    @Override
    public Collection<Ref> tags() {
        try {
            return buildSshCommand(Git.lsRemoteRepository()
                .setTags(true)
                .setTimeout(GIT_TRANS_TIMEOUT)
                .setRemote(gitUrl)).call();
        } catch (GitAPIException e) {
            throw new GitException("Fail to list tags from remote repo", e);
        }
    }

    private <T extends TransportCommand> T buildSshCommand(T command) {
        command.setTransportConfigCallback(transportConfigCallback);
        return command;
    }

    private void initSessionFactory() {
        JschConfigSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
            @Override
            protected void configure(Host host, Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
            }

            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch jSch = super.createDefaultJSch(fs);

                // apply customized private key
                if (privateKeyPath != null) {
                    jSch.removeAllIdentity();
                    jSch.addIdentity(privateKeyPath.toString());
                }

                return jSch;
            }
        };

        transportConfigCallback = transport -> {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(sshSessionFactory);
        };
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

    private void checkGitUrl() {
        if (Strings.isNullOrEmpty(gitUrl)) {
            throw new IllegalStateException("Please provides git url");
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
}