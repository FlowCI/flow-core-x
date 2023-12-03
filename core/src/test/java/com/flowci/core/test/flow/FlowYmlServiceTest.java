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

import com.flowci.common.exception.NotFoundException;
import com.flowci.common.exception.YmlException;
import com.flowci.common.helper.StringHelper;
import com.flowci.core.flow.domain.CreateOption;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.FlowYml;
import com.flowci.core.flow.domain.SimpleYml;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.YmlService;
import com.flowci.core.test.MockLoggedInScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yang
 */
public class FlowYmlServiceTest extends MockLoggedInScenario {

    @Autowired
    private FlowService flowService;

    @Autowired
    private YmlService ymlService;

    private Flow flow;

    @BeforeEach
    void init() throws IOException {
        var raw = StringHelper.toString(load("flow.yml"));
        var option = new CreateOption().setRawYaml(StringHelper.toBase64(raw));
        flow = flowService.create("hello", option);
    }

    @Test
    void should_get_yml() {
        var entity = ymlService.get(flow.getId());
        assertNotNull(entity);
        assertNotNull(entity.getId());
        assertEquals(flow.getId(), entity.getFlowId());
        assertEquals(1, entity.getList().size());

        var yml = entity.getList().get(0);
        assertEquals(FlowYml.DEFAULT_NAME, yml.getName());
    }

    @Test
    void should_throw_exception_if_yml_illegal_yml_format() {
        var illegalYml = new SimpleYml();
        illegalYml.setName("test");
        illegalYml.setRawInB64(StringHelper.toBase64("hell-..."));

        assertThrows(YmlException.class, () -> {
            ymlService.saveYml(flow, List.of(illegalYml));
        });
    }

    @Test
    void should_throw_exception_if_plugin_not_found() throws IOException {
        // when:
        String ymlRaw = StringHelper.toString(load("flow-with-plugin-not-found.yml"));

        var illegalYml = new SimpleYml();
        illegalYml.setName("test");
        illegalYml.setRawInB64(StringHelper.toBase64(ymlRaw));

        // then:
        assertThrows(NotFoundException.class, () -> {
            ymlService.saveYml(flow, List.of(illegalYml));
        });
    }
}
