/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.plugin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.common.git.GitClient;
import com.flowci.core.plugin.dao.PluginDao;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.domain.PluginParser;
import com.flowci.core.plugin.domain.PluginRepoInfo;
import com.flowci.core.plugin.event.RepoCloneEvent;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author yang
 */
@Log4j2
@Component
public class PluginServiceImpl implements PluginService {

    private static final TypeReference<List<PluginRepoInfo>> RepoListType = new TypeReference<List<PluginRepoInfo>>() {
    };

    private static final String PluginFileName = "plugin.yml";

    private static final String ReadMeFileName = "README.md";

    private static final byte[] EmptyBytes = new byte[0];

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Path pluginDir;

    @Autowired
    private ConfigProperties.Plugin pluginProperties;

    @Autowired
    private ThreadPoolTaskExecutor repoCloneExecutor;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PluginDao pluginDao;

    private final Object reloadLock = new Object();

    @Override
    public Collection<Plugin> list() {
        return pluginDao.findAll();
    }

    @Override
    public Plugin get(String name) {
        Optional<Plugin> optional = pluginDao.findByName(name);
        if (!optional.isPresent()) {
            throw new NotFoundException("The plugin {0} is not found", name);
        }
        return optional.get();
    }

    @Override
    public byte[] getReadMe(Plugin plugin) {
        try {
            Path path = getDir(plugin);
            return Files.readAllBytes(Paths.get(path.toString(), ReadMeFileName));
        } catch (IOException e) {
            log.warn("Unable to get plugin README.md file");
            return EmptyBytes;
        }
    }

    @Override
    public byte[] getIcon(Plugin plugin) {
        try {
            Path path = getDir(plugin);
            return Files.readAllBytes(Paths.get(path.toString(), plugin.getIcon()));
        } catch (IOException e) {
            log.warn("Unable to get plugin icon file");
            return EmptyBytes;
        }
    }

    @Override
    public Path getDir(Plugin plugin) {
        return getPluginRepoDir(plugin.getName(), plugin.getVersion().toString());
    }

    @Override
    public List<PluginRepoInfo> load(String repoUrl) {
        try {
            repoUrl = repoUrl + "?t=" + Instant.now().toEpochMilli();
            return objectMapper.readValue(new URL(repoUrl), RepoListType);
        } catch (Throwable e) {
            log.warn("Unable to load plugin repo '{}' : {}", repoUrl, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void clone(List<PluginRepoInfo> repos) {
        for (PluginRepoInfo repo : repos) {
            repoCloneExecutor.execute(() -> {
                try {
                    Plugin plugin = clone(repo);
                    saveOrUpdate(plugin);
                    context.publishEvent(new RepoCloneEvent(this, plugin));
                    log.info("Plugin {} been clone", plugin);
                } catch (Exception e) {
                    log.warn("Unable to clone plugin repo {} {}", repo.getSource(), e.getMessage());
                }
            });
        }
    }

    @Override
    public void reload() {
        synchronized (reloadLock) {
            pluginDao.deleteAll();

            String repoUrl = pluginProperties.getDefaultRepo();
            List<PluginRepoInfo> repos = load(repoUrl);
            clone(repos);
        }
    }

    @Scheduled(fixedRate = 1000 * 3600)
    public void scheduleSync() {
        if (pluginProperties.getAutoUpdate()) {
            reload();
        }
    }

    private void saveOrUpdate(Plugin pluginFromRepo) {
        Optional<Plugin> optional = pluginDao.findByName(pluginFromRepo.getName());

        if (optional.isPresent()) {
            Plugin exist = optional.get();
            exist.update(pluginFromRepo);
            pluginDao.save(exist);
            return;
        }

        pluginDao.save(pluginFromRepo);
    }

    private Plugin clone(PluginRepoInfo repo) throws Exception {
        log.info("Start to load plugin: {}", repo);
        Path dir = getPluginRepoDir(repo.getName(), repo.getVersion().toString());

        GitClient client = new GitClient(repo.getSource(), null, null);
        client.klone(dir, repo.getBranch());

        return load(dir.toFile(), repo);
    }

    /**
     * Load plugin.yml from local repo
     * and convert to Plugin object
     *
     * @throws IOException
     */
    private Plugin load(File dir, PluginRepoInfo info) throws IOException {
        Path pluginFile = Paths.get(dir.toString(), PluginFileName);
        if (!Files.exists(pluginFile)) {
            throw new NotFoundException("The 'plugin.yml' not found in plugin repo {0}", info.getSource());
        }

        byte[] ymlInBytes = Files.readAllBytes(pluginFile);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(ymlInBytes)) {
            Plugin plugin = PluginParser.parse(inputStream);

            if (!info.getName().equals(plugin.getName())) {
                throw new ArgumentException(
                        "Plugin name {0} not match the name defined in repo {1}", plugin.getName(), info.getName());
            }

            if (!info.getVersion().equals(plugin.getVersion())) {
                throw new ArgumentException(
                        "Plugin {0} version {1} not match the name defined in repo {2}",
                        plugin.getName(), plugin.getVersion().toString(), info.getVersion().toString());
            }

            // tags load from repo info obj
            plugin.setAuthor(info.getAuthor());
            plugin.setBranch(info.getBranch());
            plugin.setDescription(info.getDescription());
            plugin.setSource(info.getSource());
            plugin.setTags(info.getTags());

            return plugin;
        }
    }

    /**
     * Get plugin repo path: {plugin dir}/{repo}/{version}
     */
    private Path getPluginRepoDir(String name, String version) {
        return Paths.get(pluginDir.toString(), name, version);
    }
}
