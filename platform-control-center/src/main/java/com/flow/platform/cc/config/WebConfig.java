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

package com.flow.platform.cc.config;

import com.flow.platform.cc.resource.PropertyResourceLoader;
import com.flow.platform.core.http.converter.RawGsonMessageConverter;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.Logger;
import com.flow.platform.util.resource.AppResourceLoader;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author gy@fir.im
 */
@Configuration
@EnableWebMvc
@EnableScheduling
@ComponentScan({
    "com.flow.platform.core.controller",
    "com.flow.platform.cc.context",
    "com.flow.platform.cc.controller",
    "com.flow.platform.cc.consumer",
    "com.flow.platform.cc.service",
    "com.flow.platform.cc.dao",
    "com.flow.platform.cc.util"})
@Import({AppConfig.class})
public class WebConfig extends WebMvcConfigurerAdapter {

    private final static Logger LOGGER = new Logger(WebConfig.class);

    private final static int MAX_UPLOAD_SIZE = 20 * 1024 * 1024;

    @PostConstruct
    public void init() {
        LOGGER.trace("WebConfig init");
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() throws IOException {
        AppResourceLoader propertyLoader = new PropertyResourceLoader();

        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreResourceNotFound(Boolean.FALSE);
        configurer.setLocation(propertyLoader.find());

        return configurer;
    }

    @Bean(name = "multipartResolver")
    public MultipartResolver multipartResolver() throws IOException {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setMaxUploadSize(MAX_UPLOAD_SIZE);
        return resolver;
    }

    @Bean
    public Gson gsonConfig() {
        return Jsonable.GSON_CONFIG;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(converter -> converter.getSupportedMediaTypes().contains(MediaType.APPLICATION_JSON));

        // add default json converter
        RawGsonMessageConverter jsonConverter = new RawGsonMessageConverter();
        jsonConverter.setGsonForReader(Jsonable.GSON_CONFIG);
        jsonConverter.setGsonForWriter(Jsonable.GSON_CONFIG);
        jsonConverter.setIgnoreType(true);
        converters.add(jsonConverter);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST")
            .allowCredentials(true)
            .allowedHeaders("origin", "content-type", "accept", "x-requested-with", "authenticate");
    }
}
