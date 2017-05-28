package com.flow.platform.cc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Created by gy@fir.im on 17/05/2017.
 * Copyright fir.im
 */
@Configuration
@EnableWebMvc
@ComponentScan({
        "com.flow.platform.cc.controller",
        "com.flow.platform.cc.service",
        "com.flow.platform.cc.util"})
@Import({AppConfig.class})
public class WebConfig extends WebMvcConfigurerAdapter {

    @Bean
    public PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        String envPropertiesFile = "app.properties";
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setLocation(new ClassPathResource(envPropertiesFile));
        return configurer;
    }

}
