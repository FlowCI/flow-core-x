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
import com.flow.platform.util.git.GitClient;
import com.flow.platform.util.git.GitHttpClient;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        Path localRepoPath = doGenerateLocalRepositoryPath(plugin);
        Path remoteLocalPath = doGenerateGitCloneFolderPath(plugin);

        if (!localRepoPath.toFile().exists()) {
            throw new PluginException("Folder not exists");
        }

        if (!remoteLocalPath.toFile().exists()) {
            throw new PluginException("Folder not exists");
        }

        try {
            FileUtils.deleteDirectory(localRepoPath.toFile());
            FileUtils.deleteDirectory(remoteLocalPath.toFile());
        } catch (IOException e) {
            LOGGER.error("Uninstall Error", e);
        }

        plugin.setStatus(PluginStatus.PENDING);
        dispatchEvent(plugin, null, null);
        update(plugin);
    }

    @Override
    public Plugin update(Plugin plugin) {
        pluginStoreService.update(plugin);
        return plugin;
    }

    @Override
    public void doInstall(Plugin plugin) {

        LOGGER.traceMarker("DoInstall",
            String.format("Thread: %s Start Install Plugin %s", Thread.currentThread().getId(), plugin.getName()));
        plugin.setStatus(PluginStatus.INSTALLING);
        this.dispatchEvent(plugin, null, null);

        Path bareRepoPath = doGenerateLocalRepositoryPath(plugin);

        GitClient gitClient = new GitHttpClient(plugin.getDetails() + ".git", gitCloneFolder, "", "");

        LOGGER.traceMarker("DoInstall",
            String.format("Thread: %s Start Clone Plugin %s", Thread.currentThread().getId(), plugin.getName()));
        // when clone code
        Path path = GitHelperUtil.clone(gitClient);

        LOGGER.traceMarker("DoInstall",
            String.format("Thread: %s Start Init Local Repo Plugin %s", Thread.currentThread().getId(),
                plugin.getName()));
        // init bare repo
        GitHelperUtil.initBareGitRepository(bareRepoPath);

        // set local remote
        GitHelperUtil.setLocalRemote(path, bareRepoPath);

        LOGGER.traceMarker("DoInstall",
            String.format("Thread: %s Start Get Latest Tag %s", Thread.currentThread().getId(), plugin.getName()));
        // get latest tag
        String tag = GitHelperUtil.getLatestTag(path);

        LOGGER.traceMarker("DoInstall",
            String.format("Thread: %s Start Push Tag To Local Repo %s", Thread.currentThread().getId(),
                plugin.getName()));
        // push tag to local
        GitHelperUtil.pushTag(path, LOCAL_REMOTE, tag);

        plugin.setStatus(PluginStatus.INSTALLED);
        this.dispatchEvent(plugin, tag, bareRepoPath);

        update(plugin);
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
    }
}