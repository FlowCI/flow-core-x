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
import com.flow.platform.util.http.HttpClient;
import com.flow.platform.util.http.HttpResponse;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author yh@firim
 */
@Log4j2
@Repository
public class PluginDaoImpl implements PluginDao {

    private final static String PLUGIN_STORE_FILE = "plugin_cache.json";

    private final static Gson GSON = new GsonBuilder().create();

    private Map<String, Plugin> pluginCache = new ConcurrentHashMap<>();

    /**
     * Unique label list
     */
    private Set<String> allLabels = Collections.emptySet();

    @Autowired
    private Path gitWorkspace;

    @Autowired
    private String pluginSourceUrl;

    private Path storePath;

    @PostConstruct
    private void init() {
        this.storePath = Paths.get(gitWorkspace.toString(), PLUGIN_STORE_FILE);
        load();
    }

    @Override
    public void refresh() {
        List<Plugin> plugins = doFetchPlugins();

        allLabels = new HashSet<>(plugins.size() * 2);

        for (Plugin plugin : plugins) {
            allLabels.addAll(plugin.getLabels());

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
            cached.setSource(plugin.getSource());
            cached.setPlatform(plugin.getPlatform());
            cached.setLabels(plugin.getLabels());
            cached.setDescription(plugin.getDescription());
            cached.setLatestCommit(plugin.getLatestCommit());
        }

        dump();
    }

    @Override
    public Plugin get(String name) {
        return pluginCache.get(name);
    }

    @Override
    public Set<Plugin> list(Set<PluginStatus> status, String keyword, Set<String> labels) {
        final Set<Plugin> list = new HashSet<>(pluginCache.values());

        // filter for status
        iteratorNext(list, (iterator, plugin) -> {
            if (Objects.isNull(status)) {
                return;
            }

            if (!status.contains(plugin.getStatus())) {
                iterator.remove();
            }
        });

        // filter for labels
        iteratorNext(list, (iterator, plugin) -> {
            if (Objects.isNull(labels)) {
                return;
            }

            SetView<String> intersection = Sets.intersection(labels, plugin.getLabels());
            if (intersection.size() == 0) {
                iterator.remove();
            }
        });

        // filter for keyword
        iteratorNext(list, (iterator, plugin) -> {
            if (Objects.isNull(keyword)) {
                return;
            }

            if (!plugin.getName().contains(keyword) && !plugin.getDescription().contains(keyword)) {
                list.add(plugin);
                iterator.remove();
            }
        });

        return list;
    }

    @Override
    public Set<String> labels() {
        return ImmutableSet.<String>builder().addAll(allLabels).build();
    }

    @Override
    public Plugin update(Plugin plugin) {
        pluginCache.put(plugin.getName(), plugin);
        return plugin;
    }

    @Override
    public void load() {
        Type type = new TypeToken<Map<String, Plugin>>() {
        }.getType();

        File file = storePath.toFile();
        if (!file.exists()) {
            return;
        }

        try {
            String rawData = Files.toString(file, Charsets.UTF_8);
            pluginCache = GSON.fromJson(rawData, type);
            log.trace("Plugin data been loaded from path: " + file);
        } catch (Throwable e) {
            log.warn("Unable to load plugin data: " + e.getMessage());
        }
    }

    @Override
    public void dump() {
        try {
            Files.write(GSON.toJson(pluginCache).getBytes(), storePath.toFile());
            log.trace("Plugin data been dumped to path: " + storePath);
        } catch (IOException e) {
            throw new PluginException(e.getMessage());
        }
    }

    private void iteratorNext(Collection<Plugin> plugins, PluginIteratorConsumer consumer) {
        Iterator<Plugin> iterator = plugins.iterator();
        while(iterator.hasNext()) {
            Plugin plugin = iterator.next();
            consumer.consume(iterator, plugin);
        }
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

    private class PluginRepository {

        @SerializedName("packages")
        private List<Plugin> plugins;
    }

    private interface PluginIteratorConsumer {

        void consume(Iterator<Plugin> iterator, Plugin plugin);
    }

}
