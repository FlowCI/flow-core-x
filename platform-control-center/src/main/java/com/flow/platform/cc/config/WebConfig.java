package com.flow.platform.cc.config;

import com.flow.platform.cc.util.AppPropertyUtil;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.Logger;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */
@Configuration
@EnableWebMvc
@EnableScheduling
@ComponentScan({
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
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreResourceNotFound(Boolean.FALSE);
        configurer.setLocation(AppPropertyUtil.RESOURCE);
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

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST")
            .allowCredentials(true)
            .allowedHeaders("origin", "content-type", "accept", "x-requested-with", "authenticate");
    }
}
