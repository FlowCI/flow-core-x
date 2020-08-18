package com.flowci.docker;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.docker.domain.*;
import com.flowci.domain.ObjectWrapper;
import com.flowci.util.StringHelper;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.StreamType;
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

    private static final String FormatAsJson = "--format \"{{json .}}\"";

    private static final ObjectMapper mapper = new ObjectMapper();


    private Session session;

    private final ContainerManager containerManager = new ContainerManagerImpl();

    private final ImageManager imageManager = new ImageManagerImpl();

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
        return imageManager;
    }

    @Override
    public void close() {
        if (Objects.isNull(session)) {
            return;
        }
        session.disconnect();
    }

    private class ImageManagerImpl implements ImageManager {

        @Override
        public void pull(String image, int ignore, Consumer<String> progress) throws Exception {
            List<Image> list = findImage(image);
            if (list.size() > 0) {
                return;
            }
            String cmd = String.format("docker pull %s", image);
            Output output = runCmd(cmd, (log) -> {
                if (progress != null) {
                    progress.accept(log);
                }
            });
            throwExceptionIfError(output);
        }

        private List<Image> findImage(String image) throws Exception {
            String cmd = String.format("docker image ls --filter reference=\"%s\" %s", image, FormatAsJson);
            List<Image> list = new LinkedList<>();
            Output output = runCmd(cmd, (line) -> {
                try {
                    list.add(mapper.readValue(line, Image.class));
                } catch (JsonProcessingException ignore) {
                }
            });
            throwExceptionIfError(output);
            return list;
        }
    }

    private class ContainerManagerImpl implements ContainerManager {

        @Override
        public List<Unit> list(String statusFilter, String nameFilter) throws Exception {
            StringBuilder cmd = new StringBuilder();
            cmd.append("docker ps -a ");
            if (StringHelper.hasValue(statusFilter)) {
                cmd.append(String.format("--filter \"status=%s\" ", statusFilter));
            }
            if (StringHelper.hasValue(nameFilter)) {
                cmd.append(String.format("--filter \"name=%s\" ", nameFilter));
            }
            cmd.append(FormatAsJson);

            List<Unit> list = new LinkedList<>();
            Output output = runCmd(cmd.toString(), (line) -> {
                try {
                    Container container = mapper.readValue(line, Container.class);
                    list.add(new ContainerUnit(container));
                } catch (JsonProcessingException ignore) {
                }
            });

            throwExceptionIfError(output);
            return list;
        }

        @Override
        public InspectContainerResponse inspect(String containerId) throws Exception {
            String cmd = String.format("docker inspect %s %s", containerId, FormatAsJson);
            final ObjectWrapper<InspectContainerResponse> inspected = new ObjectWrapper<>();

            Output output = runCmd(cmd, (line) -> {
                try {
                    inspected.setValue(mapper.readValue(line, InspectContainerResponse.class));
                } catch (JsonProcessingException ignore) {
                }
            });
            throwExceptionIfError(output);

            if (!inspected.hasValue()) {
                throw new Exception(String.format("Unable to inspect container %s result", containerId));
            }

            return inspected.getValue();
        }

        @Override
        public String start(StartOption startOption) throws Exception {
            if (!(startOption instanceof ContainerStartOption)) {
                throw new IllegalArgumentException();
            }

            ContainerStartOption option = (ContainerStartOption) startOption;
            StringBuilder cmd = new StringBuilder();
            cmd.append("docker run -d ");

            if (option.hasName()) {
                cmd.append(String.format("--name %s ", option.getName()));
            }

            List<String> entrypoint = option.getEntrypoint();
            if (!entrypoint.isEmpty()) {
                cmd.append(String.format("--entrypoint %s ", entrypoint.get(0)));
            }

            option.getEnv().forEach((k, v) -> cmd.append(String.format("-e %s=%s ", k, v)));
            option.getBind().forEach((s, t) -> cmd.append(String.format("-v %s:%s ", s, t)));
            cmd.append(option.getImage());

            if (!entrypoint.isEmpty() && entrypoint.size() > 1) {
                cmd.append(" ");
                for (int i = 1; i < entrypoint.size(); i++) {
                    cmd.append(entrypoint.get(i)).append(" ");
                }
            }

            ObjectWrapper<String> cid = new ObjectWrapper<>();
            Output output = runCmd(cmd.toString(), cid::setValue);
            throwExceptionIfError(output);

            if (!cid.hasValue()) {
                throw new Exception("Unable to get container line from output");
            }

            return cid.getValue();
        }

        @Override
        public void wait(String containerId, int timeoutInSeconds, Consumer<Frame> onLog) throws Exception {
            String cmd = String.format("docker logs -f %s", containerId);
            Output output = runCmd(cmd, (log) -> {
                if (onLog != null) {
                    onLog.accept(new Frame(StreamType.RAW, log.getBytes()));
                }
            });
            throwExceptionIfError(output);
        }

        @Override
        public void stop(String containerId) throws Exception {
            InspectContainerResponse.ContainerState state = inspect(containerId).getState();
            Boolean running = state.getRunning();
            if (running != null && running) {
                String cmd = String.format("docker stop %s", containerId);
                Output output = runCmd(cmd, null);
                throwExceptionIfError(output);
            }
        }

        @Override
        public void resume(String containerId) throws Exception {
            String cmd = String.format("docker start %s", containerId);
            Output output = runCmd(cmd, null);
            throwExceptionIfError(output);
        }

        @Override
        public void delete(String containerId) throws Exception {
            String cmd = String.format("docker rm -f %s", containerId);
            Output output = runCmd(cmd, null);
            throwExceptionIfError(output);
        }
    }

    private void throwExceptionIfError(Output output) throws Exception {
        if (output.getExit() != 0) {
            throw new Exception(output.getErr());
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
