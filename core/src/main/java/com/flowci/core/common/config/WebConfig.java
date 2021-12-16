/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.adviser.CrosInterceptor;
import com.flowci.core.common.helper.JacksonHelper;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.domain.Vars;
import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EnableWebMvc
@Configuration
public class WebConfig {

    @Autowired
    private HandlerInterceptor apiAuth;

    @Autowired
    private HandlerInterceptor webAuth;

    @Autowired
    private AppProperties appProperties;

    @Bean("staticResourceDir")
    public Path staticResourceDir() {
        return appProperties.getSiteDir();
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new CrosInterceptor());

                registry.addInterceptor(webAuth)
                        .addPathPatterns("/users/**")
                        .excludePathPatterns("/users/default")
                        .addPathPatterns("/flows/**")
                        .addPathPatterns("/jobs/**")
                        .addPathPatterns("/agents/**")
                        .addPathPatterns("/hosts/**")
                        .addPathPatterns("/stats/**")
                        .addPathPatterns("/plugins/**")
                        .addPathPatterns("/secrets/**")
                        .addPathPatterns("/configs/**")
                        .addPathPatterns("/triggers/**")
                        .addPathPatterns("/system/**")
                        .addPathPatterns("/auth/logout");

                registry.addInterceptor(apiAuth)
                        .addPathPatterns("/api/**");
            }

            @Override
            public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.clear();

                ObjectMapper mapperForHttp = JacksonHelper.create();
                mapperForHttp.setMixIns(mixins());

                final List<HttpMessageConverter<?>> DefaultConverters = ImmutableList.of(
                        new ByteArrayHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(mapperForHttp),
                        new ResourceHttpMessageConverter(),
                        new AllEncompassingFormHttpMessageConverter(),
                        new StringHttpMessageConverter()
                );

                converters.addAll(DefaultConverters);
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                Path dir = appProperties.getSiteDir();
                registry.addResourceHandler("/static/**")
                        .addResourceLocations(dir.toFile().toURI().toString());
            }
        };
    }

    @Bean
    public Map<Class<?>, Class<?>> mixins() {
        Map<Class<?>, Class<?>> mixins = new HashMap<>(2);
        mixins.put(Vars.class, Vars.Mixin.class);
        mixins.put(Plugin.Meta.class, Plugin.Meta.RestResponse.class);
        return mixins;
    }
}
