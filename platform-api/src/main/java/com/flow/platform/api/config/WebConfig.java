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
import com.flow.platform.core.util.SpringContextUtil;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.resource.AppResourceLoader;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableWebMvc
@EnableScheduling
@ComponentScan({
    "com.flow.platform.api.controller",
    "com.flow.platform.api.service",
    "com.flow.platform.api.dao",
    "com.flow.platform.api.validator",
    "com.flow.platform.api.util",
    "com.flow.platform.api.consumer",
    "com.flow.platform.api.context"})
@Import({AppConfig.class})
public class WebConfig extends WebMvcConfigurerAdapter {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
//            .allowedMethods("GET", "POST", "OPTIONS")
            .allowedMethods("*")
            .allowCredentials(true)
            .allowedHeaders("origin", "content-type", "accept", "x-requested-with", "authenticate", "library");
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() throws IOException {
        AppResourceLoader propertyLoader = new PropertyResourceLoader();
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreResourceNotFound(Boolean.FALSE);
        configurer.setLocation(propertyLoader.find());
        return configurer;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(converter -> converter.getSupportedMediaTypes().contains(MediaType.APPLICATION_JSON));
        GsonHttpExposeConverter gsonHttpExposeConverter = new GsonHttpExposeConverter();
        gsonHttpExposeConverter.setGson(Jsonable.GSON_CONFIG);
        converters.add(gsonHttpExposeConverter);
    }

    @Bean
    public SpringContextUtil springContextUtil(){
        return new SpringContextUtil();
    }
}
