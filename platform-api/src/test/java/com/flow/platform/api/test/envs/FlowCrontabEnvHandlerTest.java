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

package com.flow.platform.api.test.envs;

import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.envs.EnvKey;
import com.flow.platform.api.envs.FlowEnvs;
import com.flow.platform.api.envs.handler.EnvHandler;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.context.SpringContext;
import com.flow.platform.core.exception.IllegalParameterException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class FlowCrontabEnvHandlerTest extends TestBase {

    @Autowired
    private SpringContext springContext;

    private Map<EnvKey, EnvHandler> handlerMap = new HashMap<>(10);

    @Before
    public void initHandlers() {
        String[] beanNameByType = springContext.getBeanNameByType(EnvHandler.class);
        for (String bean : beanNameByType) {
            EnvHandler envHandler = (EnvHandler) springContext.getBean(bean);
            handlerMap.put(envHandler.env(), envHandler);
        }
    }

    @Test
    public void should_handle_crontab_variable() throws Throwable {
        Node node = new Node("flow", "flow");
        node.putEnv(FlowEnvs.FLOW_TASK_CRONTAB_CONTENT, "0 0/30 * * * ?");
        node.putEnv(FlowEnvs.FLOW_TASK_CRONTAB_BRANCH, "master");

        EnvHandler envHandler = handlerMap.get(FlowEnvs.FLOW_TASK_CRONTAB_CONTENT);
        envHandler.process(node);
    }

    @Test(expected = IllegalParameterException.class)
    public void should_raise_exception_when_missing_dependent_env_var() throws Throwable {
        Node node = new Node("flow", "flow");
        node.putEnv(FlowEnvs.FLOW_TASK_CRONTAB_CONTENT, "* * * * * ?");

        EnvHandler envHandler = handlerMap.get(FlowEnvs.FLOW_TASK_CRONTAB_CONTENT);
        envHandler.process(node);
    }

    @Test(expected = IllegalParameterException.class)
    public void should_raise_exception_when_incorrect_crontab_format() throws Throwable {
        Node node = new Node("flow", "flow");
        node.putEnv(FlowEnvs.FLOW_TASK_CRONTAB_CONTENT, "* * * * * *");
        node.putEnv(FlowEnvs.FLOW_TASK_CRONTAB_BRANCH, "master");

        EnvHandler envHandler = handlerMap.get(FlowEnvs.FLOW_TASK_CRONTAB_CONTENT);
        envHandler.process(node);
    }
}
