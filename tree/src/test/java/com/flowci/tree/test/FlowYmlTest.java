/*
 * Copyright 2022 flow.ci
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

package com.flowci.tree.test;

import com.flowci.common.exception.YmlException;
import com.flowci.tree.yml.FlowYml;
import com.flowci.tree.yml.StepYml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FlowYmlTest {

    @Test
    void should_merge_flow_yml() {
        FlowYml main = new FlowYml();
        main.setName("test");

        var stepYml = new StepYml();
        stepYml.setName("step");

        FlowYml other = new FlowYml();
        other.getSteps().add(stepYml);

        main.merge(other);
        assertEquals(main.getSteps(), other.getSteps());
    }

    @Test
    void should_throw_yml_exception_on_duplicated_element() {
        var stepYml = new StepYml();
        stepYml.setName("step");

        FlowYml main = new FlowYml();
        main.setName("test");
        main.getSteps().add(stepYml);

        FlowYml other = new FlowYml();
        other.getSteps().add(stepYml);

        assertThrows(YmlException.class, () -> {
            main.merge(other);
        });
    }
}
