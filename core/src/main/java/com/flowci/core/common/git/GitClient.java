package com.flowci.core.common.git;

import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.SimpleKeyPair;
import com.flowci.domain.SimpleSecret;
import com.flowci.util.StringHelper;
import com.google.common.collect.Lists;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Log4j2
@AllArgsConstructor
public class GitClient {

    private static final String RefPrefix = "refs/heads/";

    private static final int timeoutInSecond = 20;

    private final String repoUrl;

    private final Path tmpDir;

    private final SimpleSecret secret;

    public List<String> branches() throws Exception {
        LsRemoteCommand command = Git.lsRemoteRepository()
                .setRemote(repoUrl)
                .setHeads(true)
                .setTimeout(timeoutInSecond);

        setupSecret(command);

        Collection<Ref> refs = command.call();
        List<String> branches = new LinkedList<>();

        for (Ref ref : refs) {
            String refName = ref.getName();
            branches.add(refName.substring(RefPrefix.length()));
        }

        return branches;
    }

    public void klone(Path dir, String branch) throws Exception {
        GitProgressMonitor monitor = new GitProgressMonitor(repoUrl, dir.toFile());

        if (Files.exists(dir)) {
            try (Git git = Git.open(dir.toFile())) {
                PullCommand pullCommand = git.pull()
                        .setTimeout(timeoutInSecond)
                        .setRemoteBranchName(branch)
                        .setProgressMonitor(monitor);

                setupSecret(pullCommand).call();
                return;
            } catch (GitAPIException e) {
                throw new IOException(e.getMessage());
            }
        }

        CloneCommand cloneCommand = Git.cloneRepository()
                .setDirectory(dir.toFile())
                .setTimeout(timeoutInSecond)
                .setURI(repoUrl)
                .setCloneAllBranches(false)
                .setProgressMonitor(monitor)
                .setCloneSubmodules(false)
                .setBranchesToClone(Lists.newArrayList(RefPrefix + branch))
                .setBranch(branch);
        setupSecret(cloneCommand);

        try(Git ignored = cloneCommand.call()) {

        } catch (GitAPIException e) {
            throw new IOException(e.getMessage());
        }
    }

    private TransportCommand<?, ?> setupSecret(TransportCommand<?, ?> command) throws Exception {
        if (Objects.isNull(secret)) {
            return command;
        }

        if (secret instanceof SimpleKeyPair) {
            SecretCreator ssh = new SshSecret((SimpleKeyPair) secret);
            ssh.setup(command);
        }

        if (secret instanceof SimpleAuthPair) {
            SecretCreator http = new HttpSecret((SimpleAuthPair) secret);
            http.setup(command);
        }

        return command;
    }

    private interface SecretCreator extends AutoCloseable {

        void setup(TransportCommand<?, ?> command) throws Exception;

    }

    private class SshSecret implements SecretCreator {

        private final String privateKey;

        private PrivateKeySessionFactory sessionFactory;

        SshSecret(SimpleKeyPair keyPair) {
            this.privateKey = Objects.isNull(keyPair) ? StringHelper.EMPTY : keyPair.getPrivateKey();
        }

        @Override
        public void setup(TransportCommand<?, ?> command) throws Exception {
            this.sessionFactory = new PrivateKeySessionFactory(privateKey);

            command.setTransportConfigCallback(transport -> {
                SshTransport sshTransport = (SshTransport) transport;
                sshTransport.setSshSessionFactory(sessionFactory);
            });
        }

        @Override
        public void close() throws Exception {
            if (Objects.isNull(sessionFactory)) {
                return;
            }

            sessionFactory.close();
        }
    }

    private class HttpSecret implements SecretCreator {

        private final SimpleAuthPair authPair;

        HttpSecret(SimpleAuthPair authPair) {
            this.authPair = authPair;
        }

        @Override
        public void setup(TransportCommand<?, ?> command) {
            UsernamePasswordCredentialsProvider provider = new UsernamePasswordCredentialsProvider(
                    authPair.getUsername(), authPair.getPassword());
            command.setCredentialsProvider(provider);
        }

        @Override
        public void close() {

        }
    }

    private class PrivateKeySessionFactory extends JschConfigSessionFactory implements AutoCloseable {

        private final Path tmpPrivateKeyFile = Paths.get(tmpDir.toString(), UUID.randomUUID().toString());

        PrivateKeySessionFactory(String privateKey) throws IOException {
            Files.write(tmpPrivateKeyFile, privateKey.getBytes());
        }

        @Override
        protected void configure(OpenSshConfig.Host host, Session session) {
            session.setConfig("StrictHostKeyChecking", "no");
        }

        @Override
        protected JSch createDefaultJSch(FS fs) throws JSchException {
            JSch defaultJSch = super.createDefaultJSch(fs);
            defaultJSch.addIdentity(tmpPrivateKeyFile.toString());
            return defaultJSch;
        }

        @Override
        public void close() throws IOException {
            Files.deleteIfExists(tmpPrivateKeyFile);
        }
    }
}
