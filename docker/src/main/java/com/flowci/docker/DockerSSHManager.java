package com.flowci.docker;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.docker.domain.DockerStartOption;
import com.flowci.docker.domain.SSHOption;
import com.flowci.util.StringHelper;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.jcraft.jsch.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class DockerSSHManager implements DockerManager {

    private static final int ChannelTimeout = 10 * 1000;

    private static final ObjectMapper mapper = new ObjectMapper();

    private Session session;
    
    private final ContainerManager containerManager = new ContainerManagerImpl();

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public DockerSSHManager(SSHOption option) throws Exception {
        try {
            JSch jsch = new JSch();
            jsch.addIdentity("name", option.getPrivateKey().getBytes(), null, null);

            session = jsch.getSession(option.getRemoteUser(), option.getRemoteHost(), option.getPort());
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(option.getTimeoutInSeconds() * 1000);
        } catch (JSchException e) {
            this.close();
            throw new Exception(String.format("Ssh connection error: %s", e.getMessage()));
        }
    }

    @Override
    public ContainerManager getContainerManager() {
        return containerManager;
    }

    @Override
    public ImageManager getImageManager() {
        return null;
    }

    private class ContainerManagerImpl implements ContainerManager {
        @Override
        public List<Container> list(String statusFilter, String nameFilter) throws Exception {
            StringBuilder cmd = new StringBuilder();
            cmd.append("docker ps -a ");

            if (StringHelper.hasValue(statusFilter)) {
                cmd.append(String.format("--filter \"status=%s\" ", statusFilter));
            }

            if (StringHelper.hasValue(nameFilter)) {
                cmd.append(String.format("--filter \"name=%s\" ", nameFilter));
            }

            cmd.append("--format \"{{json .}}\"");

            List<Container> list = new LinkedList<>();

            Output output = runCmd(cmd.toString(), (line) -> {
                try {
                    list.add(mapper.readValue(line, Container.class));
                } catch (JsonProcessingException ignore) {
                    ignore.printStackTrace();
                }
            });

            if (output.getExit() != 0) {
                throw new Exception(output.getErr());
            }

            return list;
        }

        @Override
        public InspectContainerResponse inspect(String containerId) throws Exception {
            return null;
        }

        @Override
        public String start(DockerStartOption option) throws Exception {
            return null;
        }

        @Override
        public void wait(String containerId, int timeoutInSeconds, Consumer<Frame> onLog) throws Exception {

        }

        @Override
        public void stop(String containerId) throws Exception {

        }

        @Override
        public void resume(String containerId) throws Exception {

        }

        @Override
        public void delete(String containerId) throws Exception {

        }
    }

    private Output runCmd(String bash, Consumer<String> handler) throws JSchException, IOException {
        if (Objects.isNull(session)) {
            throw new IllegalStateException("Please init ssh session first");
        }

        Channel channel = null;

        try {
            channel = session.openChannel("exec");
            try (PipedInputStream out = new PipedInputStream(); PipedInputStream err = new PipedInputStream()) {
                ChannelExec exec = (ChannelExec) channel;
                exec.setCommand(bash);

                exec.setOutputStream(new PipedOutputStream(out));
                exec.setErrStream(new PipedOutputStream(err));

                channel.connect(ChannelTimeout);

                return Output.of(
                        collectOutput(out, handler).toString(),
                        collectOutput(err, null).toString(),
                        channel.getExitStatus()
                );
            }
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public void close() {
        if (Objects.isNull(session)) {
            return;
        }
        session.disconnect();
    }

    private static StringBuilder collectOutput(InputStream in, Consumer<String> handler) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(in))) {
            String line;
            StringBuilder builder = new StringBuilder();

            while ((line = buffer.readLine()) != null) {
                builder.append(line);
                if (handler != null) {
                    handler.accept(line);
                }
            }

            return builder;
        }
    }

    @AllArgsConstructor(staticName = "of")
    @Getter
    private static class Output {

        final String out;

        final String err;

        final int exit;
    }
}
