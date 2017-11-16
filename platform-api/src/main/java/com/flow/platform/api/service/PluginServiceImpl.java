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

package com.flow.platform.api.service;

import static com.flow.platform.core.dao.adaptor.BaseAdaptor.GSON;

import com.flow.platform.domain.Plugin;
import com.flow.platform.util.http.HttpClient;
import com.flow.platform.util.http.HttpResponse;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */
@Service
public class PluginServiceImpl implements PluginService {

    private final static String KEY = "plugins";

    private Cache<String, List<Plugin>> pluginCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS)
        .maximumSize(1000).build();

    @Value("${plugins.repository}")
    private String repoUrl;

    @Override
    public List<Plugin> list() {
        List<Plugin> list = new ArrayList<>();
        try {
            return find();
        } catch (Throwable e) {
            return list;
        }
    }

    @Override
    public void install() {

    }


    @Override
    public void uninstall() {

    }

    private class PluginRepository {

        @SerializedName("packages")
        private List<Plugin> plugins;
    }


    private String downloadPluginInfo() {
        HttpClient httpClient = HttpClient.build(repoUrl).get();
        HttpResponse<String> response = httpClient.bodyAsString();
        String body = response.getBody();
        return body;
    }

    private List<Plugin> find() throws ExecutionException {
        List<Plugin> plugins = pluginCache.get(KEY, () -> {
            String body = downloadPluginInfo();
            PluginRepository pluginRepository = GSON.fromJson(body, PluginRepository.class);
            return pluginRepository.plugins;
        });
        return plugins;
    }
}
