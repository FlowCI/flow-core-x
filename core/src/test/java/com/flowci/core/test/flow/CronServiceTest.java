/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.test.flow;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.flowci.core.test.ZookeeperScenario;
import com.flowci.domain.ObjectWrapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author yang
 */
@Slf4j
public class CronServiceTest extends ZookeeperScenario {

    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    @Test
    void should_get_exec_time_from_crontab() throws InterruptedException {
        CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
        CronParser parser = new CronParser(definition);

        Cron cron = parser.parse("* * * * *");
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        assertNotNull(executionTime);

        ZonedDateTime now = ZonedDateTime.now();
        long seconds = executionTime.timeToNextExecution(now).get().getSeconds();
        log.info("--- {} ----", seconds);

        ObjectWrapper<Boolean> result = new ObjectWrapper<>(false);
        CountDownLatch counter = new CountDownLatch(1);

        service.schedule(() -> {
            result.setValue(true);
            counter.countDown();
        }, seconds, TimeUnit.SECONDS);

        counter.await();
        assertTrue(result.getValue());
    }
}
