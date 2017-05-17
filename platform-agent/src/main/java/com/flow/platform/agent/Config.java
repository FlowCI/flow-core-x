package com.flow.platform.agent;

/**
 * Created by gy@fir.im on 16/05/2017.
 *
 * @copyright fir.im
 */
public class Config {

    /* Config properties by using -Dxxx.xxx = xxx as JVM parameter */
    public final static String PROP_IS_DEBUG = "flow.agent.debug";
    public final static String PROP_CONCURRENT_PROC = "flow.agent.concurrent";

    public static boolean isDebug() {
        String boolStr = System.getProperty(PROP_IS_DEBUG, "false");
        return Boolean.parseBoolean(boolStr);
    }

    public static int concurrentProcNum() {
        String intStr = System.getProperty(PROP_CONCURRENT_PROC, "1");
        return Integer.parseInt(intStr);
    }
}
