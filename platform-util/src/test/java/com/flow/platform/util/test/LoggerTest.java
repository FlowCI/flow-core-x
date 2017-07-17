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

package com.flow.platform.util.test;

import com.flow.platform.util.Logger;
import org.junit.Test;

/**
 * @author gy@fir.im
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
