package com.flow.platform.agent;

/**
 * Created by gy@fir.im on 16/05/2017.
 *
 * @copyright fir.im
 */
public class Config {

    /* Config properties by using -Dxxx.xxx = xxx as JVM parameter */
    public final static String PROP_IS_DEBUG = "agent.debug";
    public final static String PROP_CONCURRENT_PROC = "agent.concurrent";

    public static boolean isDebug() {
        String boolStr = System.getProperty(PROP_IS_DEBUG);
        return tryParse(boolStr, false);
    }

    public static int concurrentProcNum() {
        String intStr = System.getProperty(PROP_CONCURRENT_PROC);
        return tryParse(intStr, 1);
    }

    private static boolean tryParse(String value, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(value);
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    private static int tryParse(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Throwable e) {
            return defaultValue;
        }
    }
}
