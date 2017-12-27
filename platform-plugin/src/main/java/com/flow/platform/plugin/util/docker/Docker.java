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

package com.flow.platform.plugin.util.docker;

import com.flow.platform.plugin.exception.PluginException;
import com.flow.platform.util.Logger;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.google.common.base.Charsets;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;

/**
 * @author yh@firim
 */
public class Docker {

    private static final Logger LOGGER = new Logger(Docker.class);

    DockerClientConfig config;
    DockerClient docker;

    public Docker() {
        initClient();
    }

    public void close() {
        try {
            docker.close();
        } catch (IOException e) {
            LOGGER.error("Docker Close Error", e);
        }
    }

    public void pull(String image) {
        LOGGER.info("Pull Image " + image + " Start");
        try {
            docker
                .pullImageCmd(image)
                .exec(new PullImageProcess())
                .awaitCompletion();
        } catch (InterruptedException e) {
            LOGGER.error("Pull Image Error", e);
            throw new PluginException("Pull Image Error");
        }
        LOGGER.info("Pull Image " + image + " Success");
    }

    public void runBuild(String image, String cmd, Path repoPath) {

        LOGGER.info("Run Build Start");
        String fileName = repoPath.getFileName().toString();
        Volume volume = new Volume("/" + fileName);

        // mvn cache
        Volume mvnVolume = new Volume("/root/.m2/");

        CreateContainerResponse container = docker
            .createContainerCmd(image)
            .withBinds(new Bind(repoPath.toString(), volume), new Bind(System.getenv("HOME") + "/.m2", mvnVolume))
            .withWorkingDir("/" + fileName)
            .withCmd("/bin/sh", "-c", cmd)
            .withTty(true)
            .exec();

        docker.startContainerCmd(container.getId()).exec();

        LoggerProcess loggingCallback = new
            LoggerProcess();

        try {
            docker
                .logContainerCmd(container.getId())
                .withStdErr(true)
                .withStdOut(true)
                .withFollowStream(true)
                .withTailAll()
                .exec(loggingCallback)
                .awaitStarted();

            loggingCallback.awaitCompletion();

            int exitCode = docker
                .waitContainerCmd(container.getId())
                .exec(new WaitContainerResultCallback())
                .awaitStatusCode();

            docker.removeContainerCmd(container.getId());
            if (exitCode != 0) {
                throw new PluginException("Build Project Error ExitCode: " + exitCode);
            }

        } catch (Throwable e) {
            docker.removeContainerCmd(container.getId());
            LOGGER.error("Docker build error ", e);
            throw new PluginException("Docker build error " + e.getMessage());
        }
        LOGGER.info("Run Build Finish");
    }

    private void initClient() {
        config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        docker = DockerClientBuilder.getInstance(config)
            .build();
    }

    private void showLog(Closeable stream) {
        new Thread(() -> {
            Reader in = null;
            StringBuilder out;
            final int bufferSize = 1024;
            final char[] buffer = new char[bufferSize];
            in = new InputStreamReader((InputStream) stream, Charsets.UTF_8);

            while (true) {
                try {
                    out = new StringBuilder();
                    Thread.sleep(1);

                    int rsz = in.read(buffer, 0, buffer.length);
                    if (rsz < 0) {
                        break;
                    }
                    out.append(buffer, 0, rsz);

                    System.out.print(out.toString());

                } catch (Throwable throwable) {
                    break;
                }
            }
        }).start();
    }

    private class LoggerProcess extends LogContainerResultCallback {

        @Override
        public void onNext(Frame item) {
            super.onNext(item);
        }

        @Override
        public void onStart(Closeable stream) {
            showLog(stream);
            super.onStart(stream);
        }

        @Override
        public void onError(Throwable throwable) {
            super.onError(throwable);
        }

        @Override
        public void onComplete() {
            super.onComplete();
        }

        @Override
        public void close() throws IOException {
            super.close();
        }
    }


    class PullImageProcess extends PullImageResultCallback {

        @Override
        public void onStart(Closeable stream) {
            showLog(stream);
            super.onStart(stream);
        }

        @Override
        public void onError(Throwable throwable) {
            super.onError(throwable);
        }

        @Override
        public void onComplete() {
            super.onComplete();
        }

        @Override
        public void close() throws IOException {
            super.close();
        }
    }
}
