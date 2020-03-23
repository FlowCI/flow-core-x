 /*
  * Copyright 2020 flow.ci
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

 package com.flowci.pool.manager;


 import com.fasterxml.jackson.annotation.JsonInclude;
 import com.fasterxml.jackson.annotation.JsonProperty;
 import com.fasterxml.jackson.core.JsonProcessingException;
 import com.fasterxml.jackson.databind.DeserializationFeature;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import com.flowci.pool.domain.AgentContainer;
 import com.flowci.pool.domain.DockerStatus;
 import com.flowci.pool.domain.SshInitContext;
 import com.flowci.pool.domain.StartContext;
 import com.flowci.pool.exception.DockerPoolException;
 import com.flowci.util.StringHelper;
 import com.jcraft.jsch.*;
 import lombok.AllArgsConstructor;
 import lombok.Getter;
 import lombok.Setter;

 import java.io.*;
 import java.util.*;
 import java.util.function.Consumer;

 import static com.flowci.pool.domain.AgentContainer.Image;
 import static com.flowci.pool.domain.AgentContainer.NameFilter;
 import static com.flowci.pool.domain.StartContext.AgentEnvs.*;

 public class SshPoolManager implements PoolManager<SshInitContext> {

     private static final int ChannelTimeout = 10 * 1000;

     private static final ObjectMapper mapper = new ObjectMapper();

     private Session session;

     static {
         mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
         mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
     }

     @Override
     public synchronized void init(SshInitContext context) throws Exception {
         try {
             JSch jsch = new JSch();
             jsch.addIdentity("name", context.getPrivateKey().getBytes(), null, null);

             session = jsch.getSession(context.getRemoteUser(), context.getRemoteHost(), 22);
             session.setConfig("StrictHostKeyChecking", "no");
             session.connect(context.getTimeoutInSeconds() * 1000);
         } catch (JSchException e) {
             this.close();
             throw new DockerPoolException("Ssh connection error: {0}", e.getMessage());
         }
     }

     @Override
     public void close() throws Exception {
         if (Objects.isNull(session)) {
             return;
         }
         session.disconnect();
     }

     @Override
     public int size() throws DockerPoolException {
         String cmd = String.format("docker ps -a --filter \"name=%s\" --format \"{{.ID}}\" | wc -l", NameFilter);
         try {
             Output output = runCmd(cmd);
             if (output.exit > 0) {
                 throw new DockerPoolException(output.err);
             }
             return Integer.parseInt(output.out.trim());
         } catch (IOException | JSchException e) {
            throw new DockerPoolException(e.getMessage());
         }
     }

     @Override
     public List<AgentContainer> list(Optional<String> state) throws DockerPoolException {
         String cmd = String.format("docker ps -a --filter \"name=%s\" --format \"{{json .}}\"", NameFilter);

         if (state.isPresent()) {
             cmd = String.format("docker ps -a --filter \"name=%s\" " +
                     "--filter \"status=%s\" " +
                     "--format \"{{json .}}\"",
                     NameFilter, state.get());
         }

         try {
             List<AgentContainer> list = new LinkedList<>();

             Output output = runCmd(cmd, (line) -> {
                 try {
                     Container c = mapper.readValue(line, Container.class);
                     list.add(AgentContainer.of(c.id, c.name, DockerStatus.toStateString(c.state)));
                 } catch (JsonProcessingException ignore) {
                 }
             });

             if (output.exit > 0) {
                 throw new DockerPoolException(output.err);
             }

             return list;
         } catch (JSchException | IOException e) {
             throw new DockerPoolException(e.getMessage());
         }
     }

     @Override
     public void start(StartContext context) throws DockerPoolException {
         try {
             String cmd = "docker run -d " +
                     String.format("--name %s ", AgentContainer.name(context.getAgentName())) +
                     String.format("-e %s=%s ", SERVER_URL, context.getServerUrl()) +
                     String.format("-e %s=%s ", AGENT_TOKEN, context.getToken()) +
                     String.format("-e %s=%s ", AGENT_LOG_LEVEL, context.getLogLevel()) +
                     String.format("-v %s:/root/.flow.ci.agent ", context.getDirOnHost()) +
                     "-v /var/run/docker.sock:/var/run/docker.sock " +
                     Image;

             Output output = runCmd(cmd);

             if (output.exit > 0) {
                 throw new DockerPoolException(output.getErr());
             }

         } catch (JSchException | IOException e) {
             throw new DockerPoolException(e.getMessage());
         }
     }

     @Override
     public void stop(String name) throws DockerPoolException {
         try {
             runCmd(String.format("docker stop %s", AgentContainer.name(name)));
         } catch (JSchException | IOException e) {
             throw new DockerPoolException(e.getMessage());
         }
     }

     @Override
     public void resume(String name) throws DockerPoolException {
         try {
             String container = AgentContainer.name(name);
             if (hasContainer(container)) {
                 runCmd("docker start " + container);
                 return;
             }
             throw new DockerPoolException("Unable to find container for agent {0}", name);
         } catch (JSchException | IOException e) {
            throw new DockerPoolException(e.getMessage());
         }
     }

     @Override
     public void remove(String name) throws DockerPoolException {
         try {
             runCmd(String.format("docker rm -f %s", AgentContainer.name(name)));
         } catch (JSchException | IOException e) {
             throw new DockerPoolException(e.getMessage());
         }
     }

     @Override
     public String status(String name) {
         String container = AgentContainer.name(name);
         try {
             if (!hasContainer(container)) {
                 return DockerStatus.None;
             }

             String cmd = String.format("docker ps -a --filter name=%s --format '{{.Status}}'", container);
             String content = runCmd(cmd).out;
             return DockerStatus.toStateString(content);

         } catch (JSchException | IOException e) {
             return DockerStatus.None;
         }
     }

     private boolean hasContainer(String cname) throws JSchException, IOException {
         String cmd = String.format("docker ps -a --filter name=%s --format '{{.Names}}'", cname);
         String content = runCmd(cmd).out;
         return StringHelper.hasValue(content);
     }

     private Output runCmd(String bash) throws IOException, JSchException {
         return runCmd(bash, null);
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

     @Getter
     @Setter
     private static class Container {

         @JsonProperty("ID")
         String id;

         @JsonProperty("Names")
         String name;

         @JsonProperty("Status")
         String state;
     }
 }