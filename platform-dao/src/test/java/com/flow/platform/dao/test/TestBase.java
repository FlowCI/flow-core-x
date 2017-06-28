package com.flow.platform.dao.test;

import com.flow.platform.dao.AgentDao;
import com.flow.platform.dao.CmdDao;
import com.flow.platform.dao.CmdResultDao;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by gy@fir.im on 23/06/2017.
 * Copyright fir.im
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {HibernateConfig.class})
public abstract class TestBase {

    @Autowired
    protected AgentDao agentDao;

    @Autowired
    protected CmdDao cmdDao;

    @Autowired
    protected CmdResultDao cmdResultDao;
}
