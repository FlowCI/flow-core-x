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

import com.flow.platform.plugin.domain.Plugin;
import com.flow.platform.plugin.domain.PluginStatus;
import com.flow.platform.plugin.exception.PluginException;
import com.flow.platform.plugin.util.FileUtil;
import com.flow.platform.plugin.util.UriUtil;
import com.flow.platform.util.http.HttpClient;
import com.flow.platform.util.http.HttpResponse;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author yh@firim
 */
public class PluginStoreServiceImpl implements PluginStoreService {

    private final static String PLUGIN_STORE_FILE = "plugin_data.log";
    private final static String PLUGIN_KEY = "plugin";
    private Map<String, Plugin> pluginCache = new HashMap<>();
    private volatile Cache<String, List<Plugin>> pluginListCache = CacheBuilder.newBuilder()
        .expireAfterAccess(12, TimeUnit.HOURS).build();
    private Path workspace;
    private Path storePath;
    private String pluginSourceUrl;

    public PluginStoreServiceImpl(Path workspace, String gitSourceUrl) {
        this.workspace = workspace;
        this.storePath = Paths.get(workspace.toString(), PLUGIN_STORE_FILE);
        this.pluginSourceUrl = gitSourceUrl;
    }

    @Override
    public Plugin find(String name) {
        loadCacheAndFetchList();
        return pluginCache.get(name);
    }

    @Override
    public List<Plugin> list() {
        loadCacheAndFetchList();
        List<Plugin> list = new LinkedList<>();
        pluginCache.forEach((name, plugin) -> list.add(plugin));
        saveCacheToFile();
        return list;
    }

    @Override
    public Plugin update(Plugin plugin) {
        loadCacheAndFetchList();
        pluginCache.put(plugin.getName(), plugin);
        saveCacheToFile();
        return plugin;
    }

    /**
     * list plugins
     * @return
     */
    private synchronized List<Plugin> doFetchPlugins() {
        try {
            return pluginListCache.get(PLUGIN_KEY, () -> {
                try {
                    HttpClient httpClient = HttpClient.build(pluginSourceUrl).get();
                    HttpResponse<String> response = httpClient.bodyAsString();

                    UriUtil.detectResponseIsOKAndThrowable(response);

                    String body = response.getBody();
                    PluginRepository pluginRepository = new GsonBuilder().serializeNulls().create()
                        .fromJson(body, PluginRepository.class);
                    return pluginRepository.plugins;
                } catch (Throwable throwable) {
                    throw new PluginException("Fetch Plugins Error ", throwable);
                }
            });
        } catch (Throwable throwable) {
            throw new PluginException("Do Fetch Plugins Happens some Error", throwable);
        }
    }

    private void doSave(List<Plugin> plugins) {
        for (Plugin plugin : plugins) {

            // only update no plugins
            if (Objects.isNull(pluginCache.get(plugin.getName()))) {
                plugin.setStatus(PluginStatus.PENDING);
                pluginCache.put(plugin.getName(), plugin);
            }
        }
    }

    private void loadCacheAndFetchList() {
        loadFileToCache();
        doSave(doFetchPlugins());
    }

    private void loadFileToCache() {
        Type type = new TypeToken<Map<String, Plugin>>() {
        }.getType();
        if (!Objects.isNull(FileUtil.read(type, storePath))) {
            pluginCache = FileUtil.read(type, storePath);
        }
    }

    private void saveCacheToFile() {
        FileUtil.write(pluginCache, storePath);
    }

    private class PluginRepository {

        @SerializedName("packages")
        private List<Plugin> plugins;
    }

}
