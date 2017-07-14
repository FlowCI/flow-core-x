package com.flow.platform.util.test;

import com.flow.platform.util.Logger;
import org.junit.Test;

/**
 * Created by gy@fir.im on 10/06/2017.
 * Copyright fir.im
 */
public class LoggerTest {

    private final static Logger logger = new Logger(LoggerTest.class);

    @Test
    public void should_print_log() {
        logger.traceMarker("should_print_log", "my formatter test %s", 123);
        logger.infoMarker("should_print_log", "my formatter test %s", 123);
        logger.warnMarker("should_print_log", "my formatter test %s", 123);
        logger.errorMarker("should_print_log", "my formatter test", new Exception("test exception"));
        logger.errorMarker("should_print_log", "my formatter test", null);
        logger.debugMarker("should_print_log", "my formatter test %s", 123);
    }
}
