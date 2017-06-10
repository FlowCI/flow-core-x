package com.flow.platform.util.logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;


/**
 * Created by gy@fir.im on 10/06/2017.
 * Copyright fir.im
 */
public class Logger {

    private org.apache.logging.log4j.Logger logger = null;

    public Logger(Class clazz) {
        logger = LogManager.getLogger(clazz);
    }

    public void trace(String message, Object ...params) {
        logger.trace(message, params);
    }

    public void trace(String method, String message, Object ...params) {
        logger.trace(MarkerManager.getMarker(method), message, params);
    }

    public void error(String message, Throwable e) {
        logger.error(message, e);
    }

    public void error(String method, String message, Throwable e) {
        logger.error(MarkerManager.getMarker(method), message, e);
    }

    public void error(String method, String message, Object ...params) {
        logger.error(MarkerManager.getMarker(method), message, params);
    }

    public void warn(String message, Object ...params) {
        logger.warn(message, params);
    }

    public void warn(String method, String message, Object ...params) {
        logger.warn(MarkerManager.getMarker(method), message, params);
    }

    public void debug(String message, Object ...params) {
        logger.debug(message, params);
    }

    public void debug(String method, String message, Object ...params) {
        logger.debug(MarkerManager.getMarker(method), message, params);
    }
}
