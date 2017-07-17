/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;

/**
 * @author gy@fir.im
 */
public class Logger {

    private org.apache.logging.log4j.Logger logger = null;

    public Logger(Class clazz) {
        logger = LogManager.getLogger(clazz.getSimpleName());
    }

    public void trace(String message) {
        logger.trace(message);
    }

    public void trace(String message, Object ...params) {
        logger.trace(String.format(message, params));
    }

    public void traceMarker(String method, String message, Object ...params) {
        logger.trace(MarkerManager.getMarker(method), String.format(message, params));
    }

    public void info(String message) {
        logger.info(message);
    }

    public void info(String message, Object... params) {
        logger.info(String.format(message, params));
    }

    public void infoMarker(String method, String message, Object... params) {
        logger.info(MarkerManager.getMarker(method), String.format(message, params));
    }

    public void error(String message, Throwable e) {
        logger.error(message, e);
    }

    public void errorMarker(String method, String message, Throwable e) {
        logger.error(MarkerManager.getMarker(method), message, e);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void warn(String message, Object... params) {
        logger.warn(String.format(message, params));
    }

    public void warnMarker(String method, String message, Object... params) {
        logger.warn(MarkerManager.getMarker(method), String.format(message, params));
    }

    public void debug(String message) {
        logger.debug(message);
    }

    public void debug(String message, Object... params) {
        logger.debug(String.format(message, params));
    }

    public void debugMarker(String method, String message, Object... params) {
        logger.debug(MarkerManager.getMarker(method), String.format(message, params));
    }
}