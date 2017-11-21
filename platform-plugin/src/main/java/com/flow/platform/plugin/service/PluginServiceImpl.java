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
import com.flow.platform.util.http.HttpClient;
import com.flow.platform.util.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * @author yh@firim
 */
public class PluginServiceImpl implements PluginService {

    private String pluginSourceUrl;

    private Map<String, Plugin> mockPluginStore = new HashMap<>();

    private Path gitCloneFolder;

    // local library
    private Path gitLocalFolder;

    private ThreadPoolExecutor threadPoolExecutor;

    private BlockingQueue<Plugin> pluginBlockingQueue = new LinkedBlockingDeque<>();

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
    public Plugin find(String name) {
        doBuildList();
        return mockPluginStore.get(name);
    }

    @Override
    public List<Plugin> list() {
        doBuildList();
        List<Plugin> list = new LinkedList<>();
        mockPluginStore.forEach((name, plugin) -> list.add(plugin));
        return list;
    }

    @Override
    public void install(String pluginName) {
        Plugin plugin = find(pluginName);

        if (Objects.isNull(plugin)) {
            throw new PluginException("not found plugin, please ensure the plugin name is exist");
        }

        // not finish can install plugin
        if (!Plugin.RUNNING_AND_FINISH_STATUS.contains(plugin)) {
            doEnqueue(plugin);
        }

    }

    @Override
    public void uninstall(String name) {

    }

    @Override
    public Plugin update(Plugin plugin) {
        mockPluginStore.put(plugin.getName(), plugin);
        return plugin;
    }

    private void doEnqueue(Plugin plugin) {
        try {
            pluginBlockingQueue.put(plugin);
        } catch (Throwable throwable) {
        }
    }

    private void doBuildList() {
        doSave(doFetchPlugins());
    }

    private void doInstall() {
        // git clone code

        // fetch latest tag

        // detect tag

        // git init bare tag

        // git push
    }


    /**
     * list plugins
     * @return
     */
    private List<Plugin> doFetchPlugins() {
        try {
            HttpClient httpClient = HttpClient.build(pluginSourceUrl).get();
            HttpResponse<String> response = httpClient.bodyAsString();

            UriUtil.detectResponseIsOKAndThrowable(response);

            String body = response.getBody();
            PluginRepository pluginRepository = new Gson().fromJson(body, PluginRepository.class);
            return pluginRepository.plugins;
        } catch (Throwable throwable) {
            throw new PluginException(" Fetch Plugins Info Is Error ", throwable);
        }
    }

    private void doSave(List<Plugin> plugins) {
        for (Plugin plugin : plugins) {

            // only update no plugins
            if (Objects.isNull(mockPluginStore.get(plugin.getName()))) {
                mockPluginStore.put(plugin.getName(), plugin);
            }
        }
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
