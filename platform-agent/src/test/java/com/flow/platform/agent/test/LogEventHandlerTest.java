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

package com.flow.platform.agent.test;

import com.flow.platform.agent.CmdManager;
import com.flow.platform.agent.Config;
import com.flow.platform.agent.LogEventHandler;
import com.flow.platform.cmd.Log;
import com.flow.platform.cmd.Log.Category;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdType;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author gy@fir.im
 */
public class LogEventHandlerTest extends TestBase {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @Before
    public void init() {
        System.setProperty(Config.PROP_ENABLE_REALTIME_AGENT_LOG, "false");
        Assert.assertEquals(false, Config.enableRealtimeLog());

        CmdManager.getInstance().kill();
    }

    @Test
    public void should_get_correct_format_websocket() throws Throwable {
        // given:
        Cmd cmd = new Cmd("TestZone", "TestAgent", CmdType.RUN_SHELL, "hello");
        cmd.setId(UUID.randomUUID().toString());
        LogEventHandler logEventHandler = new LogEventHandler(cmd);

        // when:
        String mockLogContent = "hello";
        String socketIoData = logEventHandler.websocketLogFormat(new Log(Log.Type.STDOUT, mockLogContent));

        // then:
        String expect = String
            .format("%s#%s#%s#%s#%s#%s", Category.DEFAULT, null, cmd.getZoneName(), cmd.getAgentName(), cmd.getId(),
                mockLogContent);
        Assert.assertEquals(expect, socketIoData);
    }
}
