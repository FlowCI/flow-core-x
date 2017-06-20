package com.flow.platform.cc.config;

import com.flow.platform.dao.AgentDaoImpl;
import com.flow.platform.dao.CmdDaoImpl;
import com.flow.platform.dao.CmdResultDaoImpl;
import com.flow.platform.domain.Agent;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Created by Will on 17/6/13.
 */

@org.springframework.context.annotation.Configuration
public class DatabaseConfig {

    @Value("${hb.connection.url}")
    private String connectionUrl;

    @Value("${hb.connection.driver_class}")
    private String connectionDriverClass;

    @Value("${hb.connection.username}")
    private String connectionUsername;

    @Value("${hb.connection.password}")
    private String connectionPassword;

    @Value("${hb.show_sql}")
    private Boolean showSql;

    @Value("${hb.hbm2ddl.auto}")
    private String hbHbm2ddlAuto;

    @Value("${hb.hibernate.enable_lazy_load_no_trans}")
    private Boolean enableLazyLoadNoTrans;

    @Value("${hb.hibernate.dialect}")
    private String connectionDialect;

    @Value("${hb.connection.provider_class}")
    private String c3p0ConnectionProvider;

    @Value("${hb.c3p0.max_size}")
    private String c3p0MaxSize;

    @Value("${hb.c3p0.min_size}")
    private String c3p0MinSize;

    @Value("${hb.c3p0.timeout}")
    private String c3p0Timeout;

    @Autowired
    private SessionFactory sessionFactory;

    @Bean(name = "sessionFactory")
    public SessionFactory sessionFactory() {
        Configuration configuration = new Configuration();
        configuration.setProperty("hibernate.connection.url", connectionUrl)
                .setProperty("hibernate.connection.driver_class", connectionDriverClass)
                .setProperty("hibernate.connection.username", connectionUsername)
                .setProperty("hibernate.show_sql", showSql.toString())
                .setProperty("hibernate.hbm2ddl.auto", hbHbm2ddlAuto)
                .setProperty("hibernate.dialect", connectionDialect)
                .setProperty("hibernate.connection.password", connectionPassword)
                .setProperty("hibernate.connection.provider_class", c3p0ConnectionProvider)
                .setProperty("hibernate.c3p0.max_size", c3p0MaxSize)
                .setProperty("hibernate.c3p0.min_size", c3p0MinSize)
                .setProperty("hibernate.c3p0.timeout", c3p0Timeout);
        configuration.addResource("Agent.hbm.xml")
                .addResource("CmdResult.hbm.xml")
                .addResource("Cmd.hbm.xml");
        SessionFactory sessionFactory = configuration.buildSessionFactory();
        return sessionFactory;
    }

    @Bean
    public AgentDaoImpl agentDao(){
        AgentDaoImpl agentDao = new AgentDaoImpl();
        agentDao.setSessionFactory(sessionFactory);
        return agentDao;
    }

    @Bean
    public CmdResultDaoImpl cmdResultDao(){
        CmdResultDaoImpl cmdResultDao = new CmdResultDaoImpl();
        cmdResultDao.setSessionFactory(sessionFactory);
        return cmdResultDao;
    }

    @Bean(name = "cmdDao")
    public CmdDaoImpl cmdDao(){
        CmdDaoImpl cmdDao = new CmdDaoImpl();
        cmdDao.setSessionFactory(sessionFactory);
        return cmdDao;
    }
}
