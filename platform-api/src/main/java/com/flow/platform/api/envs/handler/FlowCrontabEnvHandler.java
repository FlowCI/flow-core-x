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

package com.flow.platform.api.envs.handler;

import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.envs.EnvKey;
import com.flow.platform.api.envs.FlowEnvs;
import com.flow.platform.api.service.node.NodeCrontabService;
import com.flow.platform.core.exception.IllegalParameterException;
import com.google.common.collect.Sets;
import java.text.ParseException;
import java.util.Set;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Component
public class FlowCrontabEnvHandler extends EnvHandler {

    @Autowired
    private NodeCrontabService nodeCrontabService;

    @Override
    public EnvKey env() {
        return FlowEnvs.FLOW_TASK_CRONTAB_CONTENT;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public Set<EnvKey> dependents() {
        return Sets.newHashSet(FlowEnvs.FLOW_TASK_CRONTAB_BRANCH);
    }

    @Override
    void onHandle(Node node, String value) {
        try {
            new CronExpression(value);

            // setup crontab task
            nodeCrontabService.set(node);
        } catch (ParseException e) {
            throw new IllegalParameterException("Illegal FLOW_TASK_CRONTAB_CONTENT format: " + e.getMessage());
        }
    }

    @Override
    void onUnHandle(Node node, String value) {
        nodeCrontabService.delete(node);
    }
}
