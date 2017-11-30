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

package com.flow.platform.plugin.dao;

import com.flow.platform.plugin.domain.Plugin;
import com.flow.platform.plugin.domain.PluginStatus;
import com.flow.platform.plugin.exception.PluginException;
import com.flow.platform.plugin.util.FileUtil;
import com.flow.platform.util.Logger;
import com.flow.platform.util.http.HttpClient;
import com.flow.platform.util.http.HttpResponse;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author yh@firim
 */

@Repository
public class PluginDaoImpl implements PluginDao {

    private final static String PLUGIN_STORE_FILE = "plugin_cache.json";

    private final static Logger LOGGER = new Logger(PluginDao.class);

    private final static Gson GSON = new GsonBuilder().create();

    private Map<String, Plugin> pluginCache = new ConcurrentHashMap<>();

    @Autowired
    private Path gitWorkspace;

    @Autowired
    private String pluginSourceUrl;

    private Path storePath;

    @PostConstruct
    private void init() {
        this.storePath = Paths.get(gitWorkspace.toString(), PLUGIN_STORE_FILE);
        loadFileToCache();
    }

    @Override
    public void refreshCache() {
        List<Plugin> plugins = doFetchPlugins();

        for (Plugin plugin : plugins) {
            Plugin cached = pluginCache.get(plugin.getName());

            // only update no plugins
            if (Objects.isNull(cached)) {
                plugin.setStatus(PluginStatus.PENDING);
                pluginCache.put(plugin.getName(), plugin);
                continue;
            }

            // copy latest plugin data to cached
            cached.setAuthor(plugin.getAuthor());
            cached.setTag(plugin.getTag());
            cached.setDetails(plugin.getDetails());
            cached.setPlatform(plugin.getPlatform());
            cached.setLabels(plugin.getLabels());
        }
    }

    @Override
    public Plugin get(String name) {
        return pluginCache.get(name);
    }

    @Override
    public List<Plugin> list(PluginStatus... status) {
        // statues length is 0
        if (Objects.equals(0, status.length)) {
            return Lists.newArrayList(pluginCache.values());
        }

        List<Plugin> list = new LinkedList<>();

        for (PluginStatus item : status) {
            pluginCache.forEach((name, plugin) -> {
                if (Objects.equals(item, plugin.getStatus())) {
                    list.add(plugin);
                }
            });
        }

        return list;
    }

    @Override
    public Plugin update(Plugin plugin) {
        pluginCache.put(plugin.getName(), plugin);
        return plugin;
    }

    @Override
    public void dumpCacheToFile() {
        FileUtil.write(pluginCache, storePath);
    }

    /**
     * Load plugin list from remote url
     *
     * @return Plugin list, the item without status
     */
    private List<Plugin> doFetchPlugins() {
        try {
            HttpClient httpClient = HttpClient.build(pluginSourceUrl).get();
            HttpResponse<String> response = httpClient.bodyAsString();

            if (!response.hasSuccess()) {
                throw new PluginException(String
                    .format("status code is not 200, status code is %s, exception info is %s",
                        response.getStatusCode(),
                        response.getExceptions()));
            }

            String body = response.getBody();
            PluginRepository pluginRepository = GSON.fromJson(body, PluginRepository.class);
            return pluginRepository.plugins;
        } catch (Throwable throwable) {
            throw new PluginException("Fetch Plugins Error ", throwable);
        }
    }

    private Boolean loadFileToCache() {
        Type type = new TypeToken<Map<String, Plugin>>() {
        }.getType();

        if (!Objects.isNull(FileUtil.read(type, storePath))) {
            pluginCache = FileUtil.read(type, storePath);
            return true;
        }

        return false;
    }

    private class PluginRepository {

        @SerializedName("packages")
        private List<Plugin> plugins;
    }

}
