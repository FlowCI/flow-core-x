package com.flow.platform.cc.test.dao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Created by gy@fir.im on 24/06/2017.
 * Copyright fir.im
 */
@Configuration
@ComponentScan({"com.flow.platform.cc.dao"})
@ImportResource({"classpath:hibernate-mysql.config.xml"})
@EnableTransactionManagement
@PropertySource({"classpath:app-test.properties"})
public class HibernateConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
