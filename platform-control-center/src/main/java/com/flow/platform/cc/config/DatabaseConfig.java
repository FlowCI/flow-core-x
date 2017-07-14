package com.flow.platform.cc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Created by Will on 17/6/13.
 */
@Configuration
@ImportResource({"classpath:hibernate-mysql.config.xml"})
@EnableTransactionManagement
public class DatabaseConfig {

}
