/*
 * Copyright 2019 flow.ci
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

import com.flowci.core.flow.domain.CreateOption;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.SimpleYml;
import com.flowci.core.flow.domain.FlowYml;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.YmlService;
import com.flowci.core.test.MockLoggedInScenario;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.YmlException;
import com.flowci.util.StringHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

/**
 * @author yang
 */
public class FlowYmlServiceTest extends MockLoggedInScenario {

    @Autowired
    private FlowService flowService;

    @Autowired
    private YmlService ymlService;

    private Flow flow;

    @Before
    public void init() throws IOException {
        var raw = StringHelper.toString(load("flow.yml"));
        var option = new CreateOption().setRawYaml(StringHelper.toBase64(raw));
        flow = flowService.create("hello", option);
    }

    @Test
    public void should_get_yml() {
        var ymlList = ymlService.get(flow.getId());
        Assert.assertNotNull(ymlList);
        Assert.assertEquals(1, ymlList.size());

        var entity = ymlList.get(0);
        Assert.assertNotNull(entity.getId());
        Assert.assertEquals(flow.getId(), entity.getFlowId());
        Assert.assertEquals(FlowYml.DEFAULT_NAME, entity.getName());
    }

    @Test(expected = YmlException.class)
    public void should_throw_exception_if_yml_illegal_yml_format() {
        var illegalYml = new SimpleYml();
        illegalYml.setName("test");
        illegalYml.setRawInB64(StringHelper.toBase64("hell-..."));

        ymlService.saveYml(flow, illegalYml);
    }

    @Test(expected = NotFoundException.class)
    public void should_throw_exception_if_plugin_not_found() throws IOException {
        // when:
        String ymlRaw = StringHelper.toString(load("flow-with-plugin-not-found.yml"));

        var illegalYml = new SimpleYml();
        illegalYml.setName("test");
        illegalYml.setRawInB64(StringHelper.toBase64(ymlRaw));

        // then:
        ymlService.saveYml(flow, illegalYml);
    }
}
