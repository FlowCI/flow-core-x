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

package com.flowci.parser.v1;

import com.flowci.exception.YmlException;
import com.flowci.parser.v1.yml.FlowYml;
import com.flowci.parser.v1.yml.StepYml;
import org.junit.Assert;
import org.junit.Test;

public class FlowYmlTest {

    @Test
    public void should_merge_flow_yml() {
        FlowYml main = new FlowYml();
        main.setName("test");

        var stepYml = new StepYml();
        stepYml.setName("step");

        FlowYml other = new FlowYml();
        other.getSteps().add(stepYml);

        main.merge(other);
        Assert.assertEquals(main.getSteps(), other.getSteps());
    }

    @Test(expected = YmlException.class)
    public void should_throw_yml_exception_on_duplicated_element() {
        var stepYml = new StepYml();
        stepYml.setName("step");

        FlowYml main = new FlowYml();
        main.setName("test");
        main.getSteps().add(stepYml);

        FlowYml other = new FlowYml();
        other.getSteps().add(stepYml);

        main.merge(other);
    }
}
