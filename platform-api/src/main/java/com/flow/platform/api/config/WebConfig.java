package com.flow.platform.api.config;

import com.flow.platform.domain.Jsonable;
import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

/**
 * Created by gyfirim on 14/07/2017.
 *
 * @Copyright fir.im
 */
@Configuration
@EnableWebMvc
@EnableScheduling
@ComponentScan({
        "com.flow.platform.api.controller",
        "com.flow.platform.api.service",
        "com.flow.platform.api.dao",
        "com.flow.platform.api.util"})
@Import({AppConfig.class})
public class WebConfig extends WebMvcConfigurerAdapter {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST")
                .allowCredentials(true)
                .allowedHeaders("origin", "content-type", "accept", "x-requested-with", "authenticate");
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
                System.out.println("数据转化");
                gsonConverter.setGson(gsonConfig());
            }
        }
    }
}
