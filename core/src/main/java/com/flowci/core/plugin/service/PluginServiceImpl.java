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
import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.git.GitClient;
import com.flowci.core.common.manager.ResourceManager;
import com.flowci.core.common.manager.VarManager;
import com.flowci.core.plugin.dao.PluginDao;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.domain.PluginParser;
import com.flowci.core.plugin.event.GetPluginAndVerifySetContext;
import com.flowci.core.plugin.event.GetPluginEvent;
import com.flowci.core.plugin.event.RepoCloneEvent;
import com.flowci.domain.Input;
import com.flowci.domain.Vars;
import com.flowci.common.exception.ArgumentException;
import com.flowci.common.exception.NotFoundException;
import com.flowci.util.StringHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

/**
 * @author yang
 */
@Slf4j
@Component
public class PluginServiceImpl implements PluginService {

    private static final TypeReference<List<Plugin>> RepoListType = new TypeReference<List<Plugin>>() {
    };

    private static final String PluginFileName = "plugin.yml";

    private static final String PluginFileNameAlt = "plugin.yaml";

    private static final String ReadMeFileName = "README.md";

    private static final byte[] EmptyBytes = new byte[0];

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Path pluginDir;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private AppProperties.Plugin pluginProperties;

    @Autowired
    private TaskExecutor appTaskExecutor;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private PluginDao pluginDao;

    @Autowired
    private VarManager varManager;

    @Autowired
    private ResourceManager resourceManager;

    private final Object reloadLock = new Object();

    @PostConstruct
    public void init() {
        reload();
    }

    @EventListener
    public void onGetPluginEvent(GetPluginEvent event) {
        try {
            Plugin plugin = get(event.getName());
            event.setFetched(plugin);
            event.setDir(getDir(plugin));
        } catch (NotFoundException e) {
            event.setError(e);
            return;
        }

        if (event instanceof GetPluginAndVerifySetContext) {
            Plugin plugin = event.getFetched();
            Vars<String> context = ((GetPluginAndVerifySetContext) event).getContext();

            Optional<String> hasInvalidInput = verifyInputAndSetDefaultValue(plugin, context);
            hasInvalidInput.ifPresent(input -> {
                String p = plugin.getName();
                String value = context.get(input);
                event.setError(new ArgumentException("Illegal input {0} = {1} for plugin {2}", input, value, p));
            });
        }
    }

    /**
     * Verify input from context
     *
     * @return invalid input name, or empty if all inputs are validated
     */
    @Override
    public Optional<String> verifyInputAndSetDefaultValue(Plugin plugin, Vars<String> context) {
        for (Input input : plugin.getMeta().getInputs()) {
            String value = context.get(input.getName());

            // set default value to context
            if (!StringHelper.hasValue(value) && input.hasDefaultValue()) {
                context.put(input.getName(), input.getValue());
                value = input.getValue();
            }

            // verify value
            if (!varManager.verify(input, value)) {
                return Optional.of(input.getName());
            }
        }

        return Optional.empty();
    }

    @Override
    public Collection<Plugin> list() {
        return pluginDao.findAll();
    }

    @Override
    public Collection<Plugin> list(Set<String> tags) {
        return pluginDao.findAllByTagsIn(tags);
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
            return Files.readAllBytes(Paths.get(path.toString(), plugin.getMeta().getIcon()));
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
    public void load(Resource repoUri) {
        try {
            pluginDao.deleteAll();

            var is = resourceManager.getResource(repoUri);
            List<Plugin> plugins = objectMapper.readValue(is, RepoListType);
            pluginDao.insert(plugins);

            for (Plugin plugin : plugins) {
                appTaskExecutor.execute(() -> {
                    try {
                        clone(plugin);
                        pluginDao.save(plugin);
                        context.publishEvent(new RepoCloneEvent(this, plugin));
                        log.info("Plugin {} been clone", plugin);
                    } catch (Exception e) {
                        log.warn("Unable to clone plugin repo {} {}", plugin.getSource(), e.getMessage());
                    }
                });
            }

        } catch (Throwable e) {
            log.warn("Unable to load plugin repo '{}' : {}", repoUri, e.getMessage());
        }
    }

    @Override
    public void reload() {
        synchronized (reloadLock) {
            load(pluginProperties.getDefaultRepo());
        }
    }

    @Scheduled(initialDelay = 1000 * 3600, fixedRate = 1000 * 3600)
    public void scheduleSync() {
        if (pluginProperties.getAutoUpdate()) {
            reload();
        }
    }

    private void clone(Plugin plugin) throws Exception {
        log.info("Start to load plugin: {}", plugin);
        Path dir = getPluginRepoDir(plugin.getName(), plugin.getVersion().toString());

        String rd = appProperties.getResourceDomain();
        String source = plugin.getSourceWithDomain(rd);

        if (!StringHelper.hasValue(source)) {
            throw new NotFoundException("Plugin {0} source is missing for domain {1}", plugin.getName(), rd);
        }

        GitClient client = new GitClient(source, null, null);
        client.klone(dir, plugin.getBranch());

        setMetaFromYamlFile(dir.toFile(), plugin);

        plugin.setSynced(true);
        plugin.setSyncTime(Date.from(Instant.now()));
    }

    private void setMetaFromYamlFile(File dir, Plugin plugin) throws IOException {
        Path pluginFile = Paths.get(dir.toString(), PluginFileName);
        if (!Files.exists(pluginFile)) {
            pluginFile = Paths.get(dir.toString(), PluginFileNameAlt);
            if (!Files.exists(pluginFile)) {
                throw new NotFoundException("The 'plugin.yml' not found in plugin repo {0}", plugin.getSource());
            }
        }

        byte[] ymlInBytes = Files.readAllBytes(pluginFile);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(ymlInBytes)) {
            Plugin.Meta meta = PluginParser.parse(inputStream);

            if (!plugin.getName().equals(meta.getName())) {
                throw new ArgumentException(
                        "Plugin name {0} not match the name defined in repo {1}", plugin.getName(), plugin.getName());
            }

            if (!plugin.getVersion().equals(meta.getVersion())) {
                throw new ArgumentException(
                        "Plugin {0} version {1} not match the name defined in repo {2}",
                        plugin.getName(), plugin.getVersion().toString(), plugin.getVersion().toString());
            }

            plugin.setMeta(meta);
        }
    }

    /**
     * Get plugin repo path: {plugin dir}/{repo}/{version}
     */
    private Path getPluginRepoDir(String name, String version) {
        return Paths.get(pluginDir.toString(), name, version);
    }
}
