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

package com.flow.platform.api.config;

import com.flow.platform.api.resource.PropertyResourceLoader;
import com.flow.platform.api.util.GsonHttpExposeConverter;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.resource.AppResourceLoader;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;

@Configuration
@EnableWebMvc
@EnableScheduling
@ComponentScan({
    "com.flow.platform.api.controller",
    "com.flow.platform.api.service",
    "com.flow.platform.api.dao",
    "com.flow.platform.api.util"})
@Import({DatabaseConfig.class})
public class WebConfig extends WebMvcConfigurerAdapter {

    private final static int ASYNC_POOL_SIZE = 100;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST")
            .allowCredentials(true)
            .allowedHeaders("origin", "content-type", "accept", "x-requested-with", "authenticate");
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() throws IOException {
        AppResourceLoader propertyLoader = new PropertyResourceLoader();
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreResourceNotFound(Boolean.FALSE);
        configurer.setLocation(propertyLoader.find());
        return configurer;
    }

    @Bean
    public Gson gsonConfig() {
        return Jsonable.GSON_CONFIG;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter converter : converters) {
            // customize gson http message converter
            if (converter instanceof GsonHttpExposeConverter) {
                GsonHttpExposeConverter gsonConverter = (GsonHttpExposeConverter) converter;
                gsonConverter.setGson(gsonConfig());
            }
        }
    }

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(ASYNC_POOL_SIZE / 3);
        taskExecutor.setMaxPoolSize(ASYNC_POOL_SIZE);
        taskExecutor.setQueueCapacity(100);
        taskExecutor.setThreadNamePrefix("async-task-");
        return taskExecutor;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new ByteArrayHttpMessageConverter());
        converters.add(new StringHttpMessageConverter());
        converters.add(new ResourceHttpMessageConverter());
        converters.add(new AllEncompassingFormHttpMessageConverter());
        converters.add(new Jaxb2RootElementHttpMessageConverter());
        converters.add(new SourceHttpMessageConverter<>());
        converters.add(new GsonHttpExposeConverter());
    }

    //
    @Bean
    public HandlerAdapter handlerAdapter() {
        final AnnotationMethodHandlerAdapter handlerAdapter = new AnnotationMethodHandlerAdapter();
        handlerAdapter.setAlwaysUseFullPath(true);
        List<HttpMessageConverter<?>> converterList = new ArrayList<HttpMessageConverter<?>>();
        converterList.addAll(Arrays.asList(handlerAdapter.getMessageConverters()));
        converterList.add(new ByteArrayHttpMessageConverter());
        converterList.add(new StringHttpMessageConverter());
        converterList.add(new ResourceHttpMessageConverter());
        converterList.add(new AllEncompassingFormHttpMessageConverter());
        converterList.add(new Jaxb2RootElementHttpMessageConverter());
        converterList.add(new SourceHttpMessageConverter<>());
        converterList.add(new GsonHttpExposeConverter());
        handlerAdapter.setMessageConverters(converterList.toArray(new HttpMessageConverter<?>[converterList.size()]));
        return handlerAdapter;
    }
}
