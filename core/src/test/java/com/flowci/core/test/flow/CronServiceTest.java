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
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.YmlService;
import com.flowci.core.job.event.CreateNewJobEvent;
import com.flowci.core.test.ZookeeperScenario;
import com.flowci.domain.ObjectWrapper;
import com.flowci.util.StringHelper;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

/**
 * @author yang
 */
@Log4j2
public class CronServiceTest extends ZookeeperScenario {

    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    @Autowired
    private FlowService flowService;

    @Autowired
    private YmlService ymlService;

    @Before
    public void login() {
        mockLogin();
    }

    @Test
    public void should_get_exec_time_from_crontab() throws InterruptedException {
        CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
        CronParser parser = new CronParser(definition);

        Cron cron = parser.parse("* * * * *");
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        Assert.assertNotNull(executionTime);

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
        Assert.assertTrue(result.getValue());
    }

    @Test
    public void should_add_cron_task() throws IOException, InterruptedException {
        InputStream stream = load("flow-with-cron.yml");
        Flow flow = flowService.create("cron-test");
        ymlService.saveYml(flow, StringHelper.toString(stream));

        final CountDownLatch counter = new CountDownLatch(2);
        final ObjectWrapper<Flow> result = new ObjectWrapper<>();
        addEventListener((ApplicationListener<CreateNewJobEvent>) event -> {
            result.setValue(event.getFlow());
            counter.countDown();
        });

        counter.await();
        Assert.assertNotNull(result.getValue());
        Assert.assertEquals(flow, result.getValue());
    }

}
