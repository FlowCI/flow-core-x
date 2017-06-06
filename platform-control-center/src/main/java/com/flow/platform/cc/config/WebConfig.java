package com.flow.platform.cc.config;

import com.flow.platform.domain.Jsonable;
import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */
@Configuration
@EnableWebMvc
@EnableScheduling
@ComponentScan({
        "com.flow.platform.cc.controller",
        "com.flow.platform.cc.service",
        "com.flow.platform.cc.cloud",
        "com.flow.platform.cc.util"})
@Import({AppConfig.class})
public class WebConfig extends WebMvcConfigurerAdapter {

    private final static int MAX_UPLOAD_SIZE = 20 * 1024 * 1024;

    private final static Path UPLOAD_TEMP_FOLDER = Paths.get(System.getenv("HOME"), "upload");

    @Bean
    public PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        String envPropertiesFile = "app.properties";
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setLocation(new ClassPathResource(envPropertiesFile));
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
        for (HttpMessageConverter converter : converters) {
            // customize gson http message converter
            if (converter instanceof GsonHttpMessageConverter) {
                GsonHttpMessageConverter gsonConverter = (GsonHttpMessageConverter) converter;
                gsonConverter.setGson(gsonConfig());
            }
        }
    }

}
