package com.flow.platform.agent;

/**
 * Created by gy@fir.im on 16/05/2017.
 *
 * @copyright fir.im
 */
public class Config {

    public static boolean isDebug() {
        String boolStr = System.getProperty("agent.debug");
        return tryParse(boolStr, false);
    }

    private static boolean tryParse(String value, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
