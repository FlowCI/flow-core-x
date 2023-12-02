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

package com.flowci.core.plugin;

import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.service.PluginService;
import com.flowci.common.exception.NotFoundException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Component
public class PluginRepoResolver implements RepositoryResolver<HttpServletRequest> {

    @Autowired
    private PluginService pluginService;

    @Override
    public Repository open(HttpServletRequest req, String name) throws RepositoryNotFoundException {
        try {
            Plugin plugin = pluginService.get(name);
            File gitDir = Paths.get(pluginService.getDir(plugin).toString(), ".git").toFile();
            return new FileRepository(gitDir);
        } catch (NotFoundException | IOException e) {
            throw new RepositoryNotFoundException("Repository " + name + "does not exist");
        }
    }
}
