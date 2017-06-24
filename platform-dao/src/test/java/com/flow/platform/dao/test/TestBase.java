package com.flow.platform.dao.test;

import com.flow.platform.dao.AgentDaoImpl;
import com.flow.platform.dao.CmdDaoImpl;
import com.flow.platform.dao.CmdResultDaoImpl;
import com.flow.platform.domain.CmdResult;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.After;

import java.net.URL;

/**
 * Created by gy@fir.im on 23/06/2017.
 * Copyright fir.im
 */
public abstract class TestBase {

    private static SessionFactory factory;

    static CmdDaoImpl cmdDao;

    static AgentDaoImpl agentDao;

    static CmdResultDaoImpl cmdResultDao;

    static {
        URL resource = CmdResultDaoTest.class.getClassLoader().getResource("hibernate-ut.cfg.xml");
        factory = new Configuration().configure(resource).buildSessionFactory();

        cmdDao = new CmdDaoImpl();
        cmdDao.setSessionFactory(factory);

        cmdResultDao = new CmdResultDaoImpl();
        cmdResultDao.setSessionFactory(factory);

        agentDao = new AgentDaoImpl();
        agentDao.setSessionFactory(factory);
    }

    @After
    public void after() {
        cmdDao.baseDelete("1=1");
        cmdResultDao.baseDelete("1=1");
        agentDao.baseDelete("1=1");
    }
}
