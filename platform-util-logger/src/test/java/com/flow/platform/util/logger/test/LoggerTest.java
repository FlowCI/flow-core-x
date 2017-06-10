package com.flow.platform.util.logger.test;

import com.flow.platform.util.logger.Logger;
import org.junit.Test;

/**
 * Created by gy@fir.im on 10/06/2017.
 * Copyright fir.im
 */
public class LoggerTest {

    private final static Logger logger = new Logger(LoggerTest.class);

    @Test
    public void should_print_log() {
        logger.trace("should_print_log", "my formatter test %s", 123);
        logger.info("should_print_log", "my formatter test %s", 123);
        logger.warn("should_print_log", "my formatter test %s", 123);
        logger.error("should_print_log", "my formatter test %s", 123);
        logger.debug("should_print_log", "my formatter test %s", 123);
    }
}
