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

package com.flow.platform.plugin.test.util;

import com.flow.platform.plugin.domain.PluginDetail;
import com.flow.platform.plugin.domain.envs.PluginEnvKey;
import com.flow.platform.plugin.exception.PluginException;
import com.flow.platform.plugin.test.TestBase;
import com.flow.platform.plugin.util.YmlUtil;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class YmlUtilTest extends TestBase {

    @Test
    public void should_transfer_object_success() throws IOException {
        String ymlBody = getResource("flow-step-demo.yml");

        // when : Transfer
        PluginDetail pluginDetail = YmlUtil.fromYml(ymlBody, PluginDetail.class);

        // then: Not Null
        Assert.assertNotNull(pluginDetail);

        // then: name should equal
        Assert.assertEquals("fir-cli", pluginDetail.getName());

        // then: outputs Size is Equal
        Assert.assertEquals(2, pluginDetail.getOutputs().size());

        // then: inputs size is Equal
        Assert.assertEquals(5, pluginDetail.getInputs().size());

        PluginEnvKey stringEnvKey = pluginDetail.getInputs().get(0);
        PluginEnvKey integerEnvKey = pluginDetail.getInputs().get(2);
        PluginEnvKey booleanEnvKey = pluginDetail.getInputs().get(3);
        PluginEnvKey listEnvKey = pluginDetail.getInputs().get(4);

        // then: converter should equal instance
        Assert.assertEquals(true, stringEnvKey.converter() instanceof String);
        Assert.assertEquals(true, integerEnvKey.converter() instanceof Integer);
        Assert.assertEquals(true, booleanEnvKey.converter() instanceof Boolean);
        Assert.assertEquals(true, listEnvKey.converter() instanceof String);

        // then: should equal
        Assert.assertEquals(false, booleanEnvKey.converter());
        Assert.assertEquals(new Integer(1), integerEnvKey.converter());
        Assert.assertEquals("nanjing", listEnvKey.converter());
        Assert.assertEquals("admin", stringEnvKey.converter());
    }

    // test integer not format to integer
    @Test(expected = PluginException.class)
    public void should_transfer_object_error_integer() throws IOException {
        String ymlBody = getResource("flow-step-demo.error.integer.yml");

        YmlUtil.fromYml(ymlBody, PluginDetail.class);
    }


    // test list default not in values
    @Test(expected = PluginException.class)
    public void should_transfer_object_error_list() throws IOException {
        String ymlBody = getResource("flow-step-demo.error.list.yml");

        YmlUtil.fromYml(ymlBody, PluginDetail.class);
    }

    // test boolean default not in values
    @Test(expected = PluginException.class)
    public void should_transfer_object_error_boolean() throws IOException {
        String ymlBody = getResource("flow-step-demo.error.boolean.yml");

        PluginDetail pluginDetail = YmlUtil.fromYml(ymlBody, PluginDetail.class);
    }
}
