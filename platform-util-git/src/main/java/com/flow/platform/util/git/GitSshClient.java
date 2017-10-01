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

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.nio.file.Path;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.util.FS;

/**
 * @author yang
 */
public class GitSshClient extends AbstractGitClient {

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
    protected  <T extends TransportCommand> T buildCommand(T command) {
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
}