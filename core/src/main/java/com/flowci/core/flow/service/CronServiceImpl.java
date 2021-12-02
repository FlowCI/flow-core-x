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

package com.flowci.core.flow.service;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.manager.SpringTaskManager;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.flow.event.FlowInitEvent;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.event.CreateNewJobEvent;
import com.flowci.exception.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * @author yang
 */
@Log4j2
@Service
public class CronServiceImpl implements CronService {

    private final CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

    private final Map<String, ScheduledFuture<?>> scheduled = new ConcurrentHashMap<>();

    @Autowired
    private TaskScheduler cronScheduler;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private SpringTaskManager taskManager;

    @Autowired
    private YmlService ymlService;

    //====================================================================
    //        %% Internal events
    //====================================================================

    @EventListener
    public void initFlowCron(FlowInitEvent event) {
        for (Flow flow : event.getFlows()) {
            set(flow);
        }
    }

    //====================================================================
    //         Interface Methods
    //====================================================================

    @Override
    public void validate(String cron) {
        parser.parse(cron);
    }

    @Override
    public void set(Flow flow) {
        cancel(flow);

        if (!flow.hasCron()) {
            return;
        }

        // schedule next cron task
        String expression = "0 " + flow.getCron();
        ScheduledFuture<?> schedule = cronScheduler.schedule(new CronRunner(flow), new CronTrigger(expression));
        scheduled.put(flow.getId(), schedule);
    }

    @Override
    public void cancel(Flow flow) {
        ScheduledFuture<?> future = scheduled.get(flow.getId());
        if (future != null && !future.isCancelled()) {
            future.cancel(true);
            scheduled.remove(flow.getId());
        }
    }

    private class CronRunner implements Runnable {

        private final Flow flow;

        CronRunner(Flow flow) {
            this.flow = flow;
        }

        @Override
        public void run() {
            String expression = flow.getCron();
            String expressionBase64 = Base64.getEncoder().encodeToString(expression.getBytes());
            String taskName = String.format("%s-%s", flow.getName(), expressionBase64);

            taskManager.run(taskName, false, () -> {
                try {
                    Yml yml = ymlService.getYml(flow.getId(), Yml.DEFAULT_NAME);
                    log.info("Start flow '{}' from cron task", flow.getName());

                    eventManager.publish(new CreateNewJobEvent(this, flow, yml.getRaw(), Trigger.SCHEDULER, null));
                } catch (NotFoundException ignore) {
                    // ignore
                }
            });
        }
    }
}
