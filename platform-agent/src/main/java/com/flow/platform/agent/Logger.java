package com.flow.platform.agent;

/**
 * Created by gy@fir.im on 12/05/2017.
 * Copyright fir.im
 */
public class Logger {

    static void info(String message) {
        System.out.println(String.format("[Info:] %s", message));
    }

    static void err(Throwable e, String description) {
        System.out.println(String.format("[Err:] %s", description));
        if (e != null && e.getMessage() != null) {
            System.out.println(String.format("[Err Detail:] %s", e.getMessage()));
        }
    }

    static void warn(String message) {
        System.out.println(String.format("[Warn:] %s", message));
    }

    static void debug(String message) {
        System.out.println(String.format("[Debug:] %s", message));
    }
}
