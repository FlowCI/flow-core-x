package com.flow.platform.cc.util;
import org.hibernate.cfg.Configuration;

/**
 * Created by Will on 17/6/13.
 */
public class SessionFactoryHelper {
    private Configuration configuration;

    public SessionFactoryHelper(){
        configuration = new Configuration()
                .addClass(com.flow.platform.domain.Agent.class)
                .addClass(com.flow.platform.domain.Cmd.class)
                .addClass(com.flow.platform.domain.CmdResult.class);
        configuration.setProperty("connection.url", "${zk.connection.url}");
    }
}
