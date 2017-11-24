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
import com.flow.platform.plugin.util.GitHelperUtil;
import com.flow.platform.queue.InMemoryQueue;
import com.flow.platform.queue.PlatformQueue;
import com.flow.platform.queue.QueueListener;
import com.flow.platform.util.Logger;
import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.JGitUtil;
import com.google.common.base.Strings;
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

    private final static String ORIGIN_REMOTE = "origin";

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
        processors.add(new InitGitProcessor());
        processors.add(new FetchProcessor());
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

        dispatchEvent(PluginStatus.DELETE, null, doGenerateLocalRepositoryPath(plugin), plugin.getName());
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

        registerListener(new PluginStatusChangedConsumer(this));
    }

    private interface Processor {

        void exec(Plugin plugin);

        void clean(Plugin plugin);
    }

    private class InitGitProcessor implements Processor {

        @Override
        public void exec(Plugin plugin) {
            LOGGER.traceMarker("InitGitProcessor", "Start Init Git");
            try {
                // init bare
                JGitUtil.initBare(doGenerateGitCloneFolderPath(plugin), false);
                JGitUtil.initBare(doGenerateLocalRepositoryPath(plugin), true);

                // remote set
                JGitUtil
                    .remoteSet(doGenerateGitCloneFolderPath(plugin), ORIGIN_REMOTE, plugin.getDetails() + GIT_SUFFIX);
                JGitUtil.remoteSet(doGenerateGitCloneFolderPath(plugin), LOCAL_REMOTE,
                    doGenerateLocalRepositoryPath(plugin).toString());
            } catch (GitException e) {
                LOGGER.error("Git Init", e);
                throw new PluginException("Git Init", e);
            }
        }

        @Override
        public void clean(Plugin plugin) {
            try {
                FileUtils.deleteDirectory(doGenerateGitCloneFolderPath(plugin).toFile());
                FileUtils.deleteDirectory(doGenerateLocalRepositoryPath(plugin).toFile());
            } catch (IOException e) {
                LOGGER.error("Git Init Clean", e);
                throw new PluginException("Git Init Clean", e);
            }
        }
    }

    private class FetchProcessor implements Processor {

        @Override
        public void exec(Plugin plugin) {
            LOGGER.traceMarker("FetchProcessor", "Start Fetch Tags");
            try {
                JGitUtil.fetchTags(doGenerateGitCloneFolderPath(plugin), ORIGIN_REMOTE);
            } catch (GitException e) {
                LOGGER.error("Git Fetch", e);
                throw new PluginException("Git Fetch", e);
            }
        }

        @Override
        public void clean(Plugin plugin) {

        }
    }

    private class PushProcessor implements Processor {

        @Override
        public void exec(Plugin plugin) {
            LOGGER.traceMarker("PushProcessor", "Start Push Tags");
            try {
                Path gitPath = doGenerateGitCloneFolderPath(plugin);
                Path gitLocalPath = doGenerateLocalRepositoryPath(plugin);
                String latestGitTag = GitHelperUtil.getLatestTag(gitPath);
                String latestLocalGitTag = GitHelperUtil.getLatestTag(gitLocalPath);
                JGitUtil.push(gitPath, LOCAL_REMOTE, latestGitTag);

                if (Strings.isNullOrEmpty(latestLocalGitTag)) {
                    dispatchEvent(PluginStatus.INSTALLED, latestGitTag, gitLocalPath, plugin.getName());
                }

                if (Objects.equals(latestGitTag, latestLocalGitTag)) {
                    dispatchEvent(PluginStatus.UPDATE, latestGitTag, gitLocalPath, plugin.getName());
                }
            } catch (GitException e) {
                LOGGER.error("Git Push", e);
                throw new PluginException("Git Push", e);
            }
        }

        @Override
        public void clean(Plugin plugin) {

        }
    }
}
