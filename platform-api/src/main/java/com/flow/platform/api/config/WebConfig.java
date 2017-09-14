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
import com.flow.platform.api.security.AuthenticationInterceptor;
import com.flow.platform.api.security.token.JwtTokenGenerator;
import com.flow.platform.api.security.token.TokenGenerator;
import com.flow.platform.core.http.converter.RawGsonMessageConverter;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.resource.AppResourceLoader;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableWebMvc
@EnableScheduling
@ComponentScan({
    "com.flow.platform.api.controller",
    "com.flow.platform.api.service",
    "com.flow.platform.api.security",
    "com.flow.platform.api.dao",
    "com.flow.platform.api.context",
    "com.flow.platform.api.util",
    "com.flow.platform.api.consumer",
    "com.flow.platform.api.context"})
@Import({AppConfig.class})
public class WebConfig extends WebMvcConfigurerAdapter {

    private final static int MAX_UPLOAD_SIZE = 2 * 1024 * 1024;

    public final static Gson GSON_CONFIG_FOR_RESPONE = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdaptor())
        .create();

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
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

    @Bean(name = "multipartResolver")
    public MultipartResolver multipartResolver() throws IOException {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setMaxUploadSize(MAX_UPLOAD_SIZE);
        return resolver;
    }

    @Bean
    public TokenGenerator tokenGenerator() {
        return new JwtTokenGenerator("MY_SECRET_KEY");
    }

    @Bean
    public AuthenticationInterceptor authInterceptor() {
        List<RequestMatcher> matchers = Lists.newArrayList(
            new AntPathRequestMatcher("/flows/**")
        );
        return new AuthenticationInterceptor(matchers);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor());
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(converter -> converter.getSupportedMediaTypes().contains(MediaType.APPLICATION_JSON));

        RawGsonMessageConverter jsonConverter = new RawGsonMessageConverter();
        jsonConverter.setGsonForWriter(GSON_CONFIG_FOR_RESPONE);
        jsonConverter.setGsonForReader(Jsonable.GSON_CONFIG);
        jsonConverter.setIgnoreType(true);
        converters.add(jsonConverter);
    }

    /**
     * Used for convert zoned date time to timestamp
     */
    private static class ZonedDateTimeAdaptor extends TypeAdapter<ZonedDateTime> {

        @Override
        public void write(JsonWriter out, ZonedDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.toEpochSecond());
        }

        @Override
        public ZonedDateTime read(JsonReader in) throws IOException {
            Long ts = in.nextLong();
            Instant i = Instant.ofEpochSecond(ts);
            ZonedDateTime z;
            z = ZonedDateTime.ofInstant(i, ZoneId.systemDefault());
            return z;
        }
    }
}
