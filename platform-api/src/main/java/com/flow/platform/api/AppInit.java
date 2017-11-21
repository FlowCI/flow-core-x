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

package com.flow.platform.api;

import com.flow.platform.api.config.WebConfig;
import com.flow.platform.api.config.WebSocketConfig;
import com.flow.platform.api.resource.PropertyResourceLoader;
import com.flow.platform.util.resource.AppResourceLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;
import org.eclipse.jgit.http.server.GitServlet;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Created by gyfirim on 14/07/2017.
 */
public class AppInit implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();

        applicationContext.register(WebConfig.class, WebSocketConfig.class);
        applicationContext.setServletContext(servletContext);

        // Add the mvcServlet mapping manually and make it initialize automatically
        DispatcherServlet dispatcherServlet = new DispatcherServlet(applicationContext);
        Dynamic mvcServlet = servletContext.addServlet("mvc-dispatcher", dispatcherServlet);

        // set app property resource
        try {
            AppResourceLoader propertyLoader = new PropertyResourceLoader();
            applicationContext
                .getEnvironment()
                .getPropertySources()
                .addFirst(new ResourcePropertySource(propertyLoader.find()));
        } catch (IOException e) {
            throw new ServletException(e.getMessage());
        }

        mvcServlet.addMapping("/");
        mvcServlet.setAsyncSupported(true);
        mvcServlet.setLoadOnStartup(2);

        try {
            Path gitWorkspace = Paths.get(applicationContext.getEnvironment().getProperty("api.git.workspace"));
            initGitServlet(gitWorkspace, servletContext);
        } catch (IOException e) {
            throw new ServletException(e.getMessage());
        }
    }

    private void initGitServlet(Path gitWorkspace, ServletContext servletContext) throws IOException {
        if (!Files.exists(gitWorkspace)) {
            Files.createDirectories(gitWorkspace);
        }

        // add git servlet mapping
        Dynamic gitServlet = servletContext.addServlet("git-servlet", new GitServlet());
        gitServlet.addMapping("/git/*");
        gitServlet.setInitParameter("base-path", gitWorkspace.toString());
        gitServlet.setInitParameter("export-all", "true");
        gitServlet.setAsyncSupported(true);
        gitServlet.setLoadOnStartup(1);
    }
}
