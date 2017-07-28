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

package com.flow.platform.cc;

import com.flow.platform.cc.config.WebConfig;
import com.flow.platform.cc.resource.CloudJarResourceLoader;
import com.flow.platform.cc.resource.PropertyResourceLoader;
import com.flow.platform.util.Logger;
import com.flow.platform.util.resource.AppResourceLoader;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author gy@fir.im
 */
public class AppInit implements WebApplicationInitializer {

    private final static Logger LOGGER = new Logger(AppInit.class);

    public void onStartup(ServletContext servletContext) throws ServletException {
        LOGGER.trace("Initializing Application for %s", servletContext.getServerInfo());

        // get class loader
        ClassLoader appClassLoader = appClassLoader();

        // Create ApplicationContext and set class loader
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
        applicationContext.setClassLoader(appClassLoader);

        applicationContext.register(WebConfig.class);
        Set<Class<?>> cloudClasses = CloudJarResourceLoader.configClasses(appClassLoader);
        if (!cloudClasses.isEmpty()) {
            applicationContext.register((cloudClasses.toArray(new Class<?>[cloudClasses.size()])));
        }

        applicationContext.setServletContext(servletContext);

        // set app property resource
        try {
            AppResourceLoader propertyLoader = new PropertyResourceLoader();
            applicationContext
                .getEnvironment()
                .getPropertySources()
                .addFirst(new ResourcePropertySource(propertyLoader.find()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Add the servlet mapping manually and make it initialize automatically
        DispatcherServlet dispatcherServlet = new DispatcherServlet(applicationContext);
        ServletRegistration.Dynamic servlet = servletContext.addServlet("mvc-dispatcher", dispatcherServlet);

        servlet.addMapping("/");
        servlet.setAsyncSupported(true);
        servlet.setLoadOnStartup(1);
    }

    private ClassLoader appClassLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            URL[] urls = CloudJarResourceLoader.findAllJar();
            if (urls.length > 0) {
                return new URLClassLoader(urls, loader);
            }
            return loader;
        } catch (Throwable ignore) {
            return loader;
        }
    }
}
