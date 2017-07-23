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

package com.flow.platform.api.test.service;

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.JobFlow;
import com.flow.platform.api.domain.JobStep;
import com.flow.platform.api.domain.Step;
import com.flow.platform.api.service.JobNodeService;
import com.flow.platform.api.service.JobService;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.util.ObjectUtil;
import com.sun.org.apache.regexp.internal.RE;
import java.lang.reflect.Field;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;

/**
 * @author yh@firim
 */
public class JobServiceTest extends TestBase{
    @Autowired
    JobService jobService;

    @Autowired
    NodeService nodeService;

    @Autowired
    JobNodeService jobNodeService;

    @Test
    public void should_copy_node(){
        Flow flow = new Flow();
        flow.setPath("/flow");
        flow.setName("flow");
        Step step1 = new Step();
        step1.setName("step1");
        step1.setPath("/flow/step1");
        step1.setPlugin("step1");
        step1.setAllowFailure(true);
        Step step2 = new Step();
        step2.setName("step2");
        step2.setPath("/flow/step2");
        step2.setPlugin("step2");
        step2.setAllowFailure(true);
        flow.getChildren().add(step1);
        flow.getChildren().add(step2);
        step1.setParent(flow);
        step2.setParent(flow);
        step1.setNext(step2);
        step2.setParent(step1);
        nodeService.create(flow);

        JobFlow jobFlow = jobService.createJobNode(flow.getPath());
        jobFlow.getChildren().forEach(item -> {
            Assert.assertEquals(item.getPath(), jobNodeService.find(item.getPath()).getPath());
        });
    }

}
