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
import com.flow.platform.api.resource.PropertyResourceLoader;
import com.flow.platform.util.resource.AppResourceLoader;
import java.io.IOException;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

/**
 * Created by gyfirim on 14/07/2017.
 *
 * @Copyright fir.im
 */
public class AppInit extends AbstractSecurityWebApplicationInitializer {

    @Override
    public void afterSpringSecurityFilterChain(javax.servlet.ServletContext servletContext) {
        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();

        applicationContext.register(WebConfig.class);
        applicationContext.setServletContext(servletContext);

        // Add the servlet mapping manually and make it initialize automatically
        DispatcherServlet dispatcherServlet = new DispatcherServlet(applicationContext);
        ServletRegistration.Dynamic servlet = servletContext.addServlet("mvc-dispatcher", dispatcherServlet);

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

        servlet.addMapping("/");
        servlet.setAsyncSupported(true);
        servlet.setLoadOnStartup(1);
    }
}
