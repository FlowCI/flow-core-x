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

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.flowci.core.common.config.AppProperties;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.flow.dao.YmlDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.event.CreateNewJobEvent;
import com.flowci.tree.FlowNode;
import com.flowci.tree.YmlParser;
import com.flowci.zookeeper.ZookeeperClient;
import com.flowci.zookeeper.ZookeeperException;
import lombok.extern.log4j.Log4j2;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author yang
 */
@Log4j2
@Service
public class CronServiceImpl implements CronService {

    private static final int MaxCronPoolSize = 20;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(MaxCronPoolSize);

    @Autowired
    private YmlDao ymlDao;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private AppProperties.Zookeeper zkProperties;

    @Autowired
    private ZookeeperClient zk;

    @PostConstruct
    private void initZkRoot() {
        try {
            String root = zkProperties.getCronRoot();
            zk.create(CreateMode.PERSISTENT, root, null);
        } catch (ZookeeperException ignore) {

        }
    }

    @Override
    public void update(Flow flow, FlowNode root, Yml yml) {
        if (!root.hasCron()) {
            return;
        }

        // schedule next cron task
        String expression = root.getCron();
        long delay = nextSeconds(expression);
        CronRunner runner = new CronRunner(flow, yml, expression);
        executor.schedule(runner, delay, TimeUnit.SECONDS);
    }

    private class CronRunner implements Runnable {

        private final Flow flow;

        private final Yml yml;

        private final String expression;

        private final String path;

        CronRunner(Flow flow, Yml yml, String expression) {
            this.flow = flow;
            this.yml = yml;
            this.expression = expression;
            this.path = getFlowCronPath();
        }

        @Override
        public void run() {
            if (lock()) {
                log.info("Start flow '{}' from cron task", flow.getName());
                eventManager.publish(new CreateNewJobEvent(this, flow, yml.getRaw(), Trigger.SCHEDULER, null));
                clean();
            }

            scheduleNext();
        }

        private void scheduleNext() {
            Optional<Yml> optional = ymlDao.findById(flow.getId());
            if (!optional.isPresent()) {
                return;
            }

            FlowNode node = YmlParser.load(flow.getName(), yml.getRaw());
            update(flow, node, optional.get());
        }

        /**
         * check zk and lock
         */
        private boolean lock() {
            try {
                zk.create(CreateMode.EPHEMERAL, path, null);
                return true;
            } catch (ZookeeperException e) {
                log.warn("Unable to init cron : {}", e.getMessage());
                return false;
            }
        }

        private void clean() {
            try {
                zk.delete(path, false);
            } catch (ZookeeperException ignore) {

            }
        }

        private String getFlowCronPath() {
            String expressionBase64 = Base64.getEncoder().encodeToString(expression.getBytes());
            return ZKPaths.makePath(zkProperties.getCronRoot(), flow.getName() + "-" + expressionBase64);
        }
    }

    private static long nextSeconds(String expression) {
        CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
        CronParser parser = new CronParser(definition);

        Cron cron = parser.parse(expression);
        ExecutionTime executionTime = ExecutionTime.forCron(cron);

        ZonedDateTime now = ZonedDateTime.now();
        return executionTime.timeToNextExecution(now).get().getSeconds();
    }
}
