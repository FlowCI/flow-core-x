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

package com.flowci.core.plugin.config;

import com.flowci.core.common.config.AppProperties;
import com.flowci.core.plugin.PluginRepoResolver;
import com.flowci.util.FileHelper;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jgit.http.server.GitServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author yang
 */
@Log4j2
@Configuration
public class PluginConfig {

    private static final String GIT_URL = "/git/plugins";

    @Autowired
    private AppProperties appProperties;

    @Bean("pluginDir")
    public Path pluginDir() throws IOException {
        String workspace = appProperties.getWorkspace().toString();
        Path pluginDir = Paths.get(workspace, "plugins");
        return FileHelper.createDirectory(pluginDir);
    }

    @Bean("gitServletBean")
    public ServletRegistrationBean<GitServlet> gitServletBean(Path pluginDir, PluginRepoResolver pluginRepoResolver) {
        GitServlet servlet = new GitServlet();
        servlet.setRepositoryResolver(pluginRepoResolver);

        ServletRegistrationBean<GitServlet> bean = new ServletRegistrationBean<>(servlet, GIT_URL + "/*");
        bean.setLoadOnStartup(1);
        bean.addInitParameter("base-path", pluginDir.toString());
        bean.addInitParameter("export-all", "true");
        bean.setAsyncSupported(true);
        return bean;
    }
}
