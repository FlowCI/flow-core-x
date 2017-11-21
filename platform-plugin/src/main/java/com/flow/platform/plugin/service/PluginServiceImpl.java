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
import com.flow.platform.plugin.exception.PluginException;
import com.flow.platform.plugin.util.UriUtil;
import com.flow.platform.util.git.GitClient;
import com.flow.platform.util.git.GitException;
import com.flow.platform.util.git.GitHttpClient;
import com.flow.platform.util.http.HttpClient;
import com.flow.platform.util.http.HttpResponse;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @author yh@firim
 */
public class PluginServiceImpl implements PluginService {

    private String pluginSourceUrl;

    private final static String KEY = "plugins";

    private Map<String, Plugin> mockPluginStore = new HashMap<>();

    private Path gitCloneFolder;

    // local library
    private Path gitLocalFolder;

    private ThreadPoolExecutor threadPoolExecutor;

    private BlockingQueue<Plugin> pluginBlockingQueue = new LinkedBlockingDeque<>();

    private Cache<String, List<Plugin>> pluginCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS)
        .maximumSize(1000).build();

    public PluginServiceImpl(String repoUrl) {
        this.pluginSourceUrl = repoUrl;
    }

    private List<InstallConsumer> installerConsumerPooler = new LinkedList<>();

    public PluginServiceImpl(String pluginSourceUrl, Path workspace, Path gitLocalFolder,
                             ThreadPoolExecutor threadPoolExecutor) {
        this.pluginSourceUrl = pluginSourceUrl;
        this.gitCloneFolder = workspace;
        this.threadPoolExecutor = threadPoolExecutor;
        this.gitLocalFolder = gitLocalFolder;
        registerConsumers();
    }

    @Override
    public List<Plugin> list() {
        return combinePluginStatus(doFind());
    }

    @Override
    public void install(String pluginName) {
        Plugin plugin = findByName(pluginName);

        if (Objects.isNull(plugin)) {
            throw new PluginException("not found plugin, please ensure the plugin name is exist");
        }

        // not finish can install plugin
        if (!Plugin.RUNNING_AND_FINISH_STATUS.contains(plugin)) {
            enqueue(plugin);
        }

    }

    @Override
    public void uninstall(String name) {

    }

    @Override
    public Path workspace() {
        return gitCloneFolder;
    }

    @Override
    public Plugin update(Plugin plugin) {
        mockPluginStore.put(plugin.getName(), plugin);
        return plugin;
    }

    @Override
    public void doInstall(String pluginName) {
        Plugin plugin = findByName(pluginName);
        GitClient gitClient = new GitHttpClient(plugin.getDetails() + ".git", gitCloneFolder, "", "");

        // git clone
        gitClone(gitClient);

        // Fetch latest tag
        try {
            List<String> tags = gitClient.tags();
        } catch (GitException e) {
            throw new PluginException("when git tags, it throw some exceptions", e);
        }

        // init bared
        // push tag to local repo

        // callback
    }

    private void gitClone(GitClient gitClient) {
        try {
            gitClient.clone("master", true);
        } catch (GitException e) {
            throw new PluginException("when git clone, it throw some exceptions", e);
        }
    }

    private void initBareLibrary(String name) {

    }


    private List<Plugin> combinePluginStatus(List<Plugin> plugins) {
        for (Plugin plugin : plugins) {
            Plugin plg = mockPluginStore.get(plugin.getName());
            if (!Objects.isNull(plg)) {
                plugin.setStatus(plg.getStatus());
            }
        }

        return plugins;
    }

    private void enqueue(Plugin plugin) {
        try {
            pluginBlockingQueue.put(plugin);
        } catch (Throwable throwable) {
        }
    }

    private Plugin findByName(String name) {
        for (Plugin plugin : list()) {
            if (Objects.equals(plugin.getName(), name)) {
                return plugin;
            }
        }

        return null;
    }

    /**
     * list plugins
     * @return
     */
    private List<Plugin> doFind() {
        List<Plugin> plugins;
        try {
            plugins = pluginCache.get(KEY, () -> {
                String body = fetchPluginInfo();
                PluginRepository pluginRepository = new Gson().fromJson(body, PluginRepository.class);
                return pluginRepository.plugins;
            });
        } catch (ExecutionException e) {
            plugins = new ArrayList<>();
        } catch (Throwable throwable) {
            throw new PluginException(" Fetch Plugins Info Is Error ", throwable);
        }
        return plugins;
    }

    /**
     * fetch plugin repos info
     * @return
     */
    private String fetchPluginInfo() {
        HttpClient httpClient = HttpClient.build(pluginSourceUrl).get();
        HttpResponse<String> response = httpClient.bodyAsString();

        UriUtil.detectResponseIsOKAndThrowable(response);

        String body = response.getBody();
        return body;
    }


    private class PluginRepository {

        @SerializedName("packages")
        private List<Plugin> plugins;
    }


    private void registerConsumers() {
        for (int i = 0; i < threadPoolExecutor.getCorePoolSize(); i++) {
            InstallConsumer installConsumer = new InstallConsumer(threadPoolExecutor, pluginBlockingQueue, this);
            installConsumer.start();
            installerConsumerPooler.add(installConsumer);
        }
    }
}
