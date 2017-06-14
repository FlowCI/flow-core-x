package com.flow.platform.cc.util;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Created by Will on 17/6/13.
 */
public class SessionFactoryHelper {
    private Configuration configuration;

    public SessionFactoryHelper() throws IOException {
        InputStream resource = SessionFactoryHelper.class.getResourceAsStream("app.properties");
        Properties properties = new Properties();
        properties.load(resource);

        configuration = new Configuration();
//                .addClass(com.flow.platform.domain.Agent.class)
//                .addClass(com.flow.platform.domain.Cmd.class)
//                .addClass(com.flow.platform.domain.CmdResult.class);
        configuration.setProperty("connection.url", properties.getProperty("connection.url"))
                .setProperty("connection.driver_class", properties.getProperty("connection.driver_class"))
                .setProperty("connection.username", properties.getProperty("connection.username"))
                .setProperty("show_sql", properties.getProperty("show_sql"))
                .setProperty("hbm2ddl.auto", properties.getProperty("hbm2ddl.auto"))
                .setProperty("hibernate.enable_lazy_load_no_trans", properties.getProperty("hibernate.enable_lazy_load_no_trans"));

        sessionFactory = configuration.buildSessionFactory();
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private SessionFactory sessionFactory;
}
