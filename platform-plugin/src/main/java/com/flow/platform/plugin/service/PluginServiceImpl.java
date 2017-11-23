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

package com.flow.platform.plugin.service;

import com.flow.platform.plugin.consumer.InstallConsumer;
import com.flow.platform.plugin.consumer.PluginStatusChangedConsumer;
import com.flow.platform.plugin.domain.Plugin;
import com.flow.platform.plugin.domain.PluginStatus;
import com.flow.platform.plugin.event.AbstractEvent;
import com.flow.platform.plugin.event.PluginListener;
import com.flow.platform.plugin.exception.PluginException;
import com.flow.platform.plugin.util.FileUtil;
import com.flow.platform.plugin.util.GitHelperUtil;
import com.flow.platform.queue.InMemoryQueue;
import com.flow.platform.queue.PlatformQueue;
import com.flow.platform.queue.QueueListener;
import com.flow.platform.util.Logger;
import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.JGitUtil;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import org.apache.commons.io.FileUtils;


/**
 * @author yh@firim
 */
public class PluginServiceImpl extends AbstractEvent implements PluginService {

    private String pluginSourceUrl;

    private final static String PLUGIN_KEY = "plugin";

    private final static String GIT_SUFFIX = ".git";

    private final static String LOCAL_REMOTE = "local";

    private final static int QUEUE_MAX_SIZE = 100000;

    private final static Logger LOGGER = new Logger(PluginService.class);

    private PlatformQueue installerQueue;

    // git clone folder
    private Path gitCloneFolder;
    // local library
    private Path gitLocalRepositoryFolder;
    // workspace
    private Path workspace;

    private ThreadPoolExecutor threadPoolExecutor;

    private PluginStoreService pluginStoreService;

    private List<Processor> processors = new LinkedList<>();

    {
        processors.add(new CloneProcessor());
        processors.add(new InitLocalRepoProcessor());
        processors.add(new PushProcessor());
    }

    public PluginServiceImpl(String pluginSourceUrl,
                             Path gitCloneFolder,
                             Path gitLocalRepositoryFolder,
                             Path workspace,
                             ThreadPoolExecutor threadPoolExecutor) {
        this.pluginSourceUrl = pluginSourceUrl;
        this.gitCloneFolder = gitCloneFolder;
        this.threadPoolExecutor = threadPoolExecutor;
        this.gitLocalRepositoryFolder = gitLocalRepositoryFolder;
        this.workspace = workspace;
        installerQueue = new InMemoryQueue<>(this.threadPoolExecutor, QUEUE_MAX_SIZE, PLUGIN_KEY);

        pluginStoreService = new PluginStoreServiceImpl(workspace, pluginSourceUrl);

        // register consumer
        initConsumers();
    }

    @Override
    public void registerListener(PluginListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public Plugin find(String name) {
        return pluginStoreService.find(name);
    }

    @Override
    public Plugin update(Plugin plugin) {
        pluginStoreService.update(plugin);
        return plugin;
    }

    @Override
    public List<Plugin> list() {
        return pluginStoreService.list();
    }

    @Override
    public void install(String pluginName) {
        Plugin plugin = find(pluginName);

        if (Objects.isNull(plugin)) {
            throw new PluginException("not found plugin, please ensure the plugin name is exist");
        }

        // not finish can install plugin
        if (!Plugin.RUNNING_AND_FINISH_STATUS.contains(plugin.getStatus())) {
            LOGGER.trace(String.format("Plugin %s Enter To Queue", pluginName));
            installerQueue.enqueue(plugin);
        }
    }

    @Override
    public void uninstall(String name) {
        Plugin plugin = find(name);

        if (Objects.isNull(plugin)) {
            throw new PluginException("not found plugin, please ensure the plugin name is exist");
        }

        // Running Plugin not uninstall
        if (Objects.equals(plugin.getStatus(), PluginStatus.INSTALLING)) {
            throw new PluginException("running plugin not install");
        }

        for (Processor processor : processors) {
            processor.clean(plugin);
        }

        plugin.setStatus(PluginStatus.PENDING);
        update(plugin);
        dispatchEvent(PluginStatus.DELETE, null, doGenerateLocalRepositoryPath(plugin));
    }

    @Override
    public void execInstall(Plugin plugin) {
        for (Processor processor : processors) {
            processor.exec(plugin);
        }
    }

    private Path doGenerateLocalRepositoryPath(Plugin plugin) {
        String[] aars = plugin.getDetails().split("/");
        return Paths.get(gitLocalRepositoryFolder.toString(), aars[aars.length - 1] + GIT_SUFFIX);
    }

    private Path doGenerateGitCloneFolderPath(Plugin plugin) {
        String[] aars = plugin.getDetails().split("/");
        return Paths.get(gitCloneFolder.toString(), aars[aars.length - 1]);
    }

    private void initConsumers() {
        QueueListener installConsumer = new InstallConsumer(installerQueue, this);

        // start many threads
        for (int i = 0; i < threadPoolExecutor.getCorePoolSize(); i++) {
            installerQueue.start();
        }

        registerListener(new PluginStatusChangedConsumer());
    }

    private interface Processor {

        void exec(Plugin plugin);

        void clean(Plugin plugin);
    }

    private class CloneProcessor implements Processor {

        @Override
        public void exec(Plugin plugin) {
            LOGGER.traceMarker("CloneProcessor",
                String.format("Thread: %s Start Clone %s", Thread.currentThread().getId(), plugin.getName()));
            plugin.setStatus(PluginStatus.INSTALLING);
            update(plugin);

            try {
                JGitUtil.clone(plugin.getDetails() + GIT_SUFFIX, doGenerateGitCloneFolderPath(plugin));
            } catch (GitException e) {
                LOGGER.error("Git Clone", e);
                throw new PluginException("Git Clone", e);
            }
        }

        @Override
        public void clean(Plugin plugin) {
            Path gitPath = doGenerateGitCloneFolderPath(plugin);
            if (!gitPath.toFile().exists()) {
                try {
                    FileUtils.deleteDirectory(gitPath.toFile());
                } catch (Throwable throwable) {
                    LOGGER
                        .error(String.format("Delete Folder: %s Error: %s", gitPath.toString(), throwable), throwable);
                }
            }
        }
    }

    private class InitLocalRepoProcessor implements Processor {

        @Override
        public void exec(Plugin plugin) {
            Path bareRepoPath = doGenerateLocalRepositoryPath(plugin);
            Path path = doGenerateGitCloneFolderPath(plugin);
            LOGGER.traceMarker("InitLocalRepoProcessor",
                String.format("Thread: %s Start Init Local Repo Plugin %s", Thread.currentThread().getId(),
                    plugin.getName()));

            try {
                JGitUtil.initBare(bareRepoPath);
                JGitUtil.remoteSet(path, LOCAL_REMOTE, bareRepoPath.toString());
            } catch (GitException e) {
                LOGGER.error("Init Local Repo Error", e);
                throw new PluginException("Init Local Repo Error", e);
            }
        }

        @Override
        public void clean(Plugin plugin) {
            Path gitPath = doGenerateLocalRepositoryPath(plugin);
            if (!gitPath.toFile().exists()) {
                try {
                    FileUtils.deleteDirectory(gitPath.toFile());
                } catch (Throwable throwable) {
                    LOGGER
                        .error(String.format("Delete Local Folder: %s Error: %s", gitPath.toString(), throwable), throwable);
                }
            }
        }
    }

    private class PushProcessor implements Processor {

        @Override
        public void exec(Plugin plugin) {
            Path bareRepoPath = doGenerateLocalRepositoryPath(plugin);
            Path path = doGenerateGitCloneFolderPath(plugin);
            // get latest tag
            String tag = GitHelperUtil.getLatestTag(path);

            LOGGER.traceMarker("PushProcessor",
                String.format("Thread: %s Start Push Tag To Local Repo %s", Thread.currentThread().getId(),
                    plugin.getName()));

            try {
                JGitUtil.push(path, LOCAL_REMOTE, tag);
            } catch (GitException e) {
                LOGGER.error("Push To Local Error", e);
                throw new PluginException("Push To Local Error", e);
            }

            plugin.setStatus(PluginStatus.INSTALLED);
            update(plugin);

            // dispatch Event
            dispatchEvent(PluginStatus.INSTALLED, tag, bareRepoPath);
        }

        @Override
        public void clean(Plugin plugin) {

        }
    }
}
