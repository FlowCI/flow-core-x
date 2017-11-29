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
import com.flow.platform.plugin.test.TestBase;
import com.flow.platform.plugin.util.YmlUtil;
import java.io.IOException;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class YmlUtilTest extends TestBase {

    @Test
    public void should_transfer_object_success() throws IOException {
        String ymlBody = getResource("flow-step-demo.yml");

        PluginDetail pluginDetail = YmlUtil.fromYml(ymlBody, PluginDetail.class);
    }
}
