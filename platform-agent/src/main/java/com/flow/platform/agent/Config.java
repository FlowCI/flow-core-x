package com.flow.platform.agent;

import com.flow.platform.domain.AgentConfig;

/**
 * Created by gy@fir.im on 16/05/2017.
 *
 * @copyright fir.im
 */
public class Config {

    // Property to control should shutdown machine after close agent
    public final static String PROP_SHOULD_SHUTDOWN = "flow.agent.should_shut_down";

    /* Config properties by using -Dxxx.xxx = xxx as JVM parameter */
    public final static String PROP_IS_DEBUG = "flow.agent.debug";
    public final static String PROP_CONCURRENT_PROC = "flow.agent.concurrent";
    public final static String PROP_SUDO_PASSWORD = "flow.agent.sudo.pwd";

    public static boolean isDebug() {
        String boolStr = System.getProperty(PROP_IS_DEBUG, "false");
        return Boolean.parseBoolean(boolStr);
    }

    public static int concurrentProcNum() {
        String intStr = System.getProperty(PROP_CONCURRENT_PROC, "1");
        return Integer.parseInt(intStr);
    }

    public static String sudoPassword() {
        return System.getProperty(PROP_SUDO_PASSWORD, "");
    }

    public static AgentConfig load(String url) {
        return null;
    }
}
