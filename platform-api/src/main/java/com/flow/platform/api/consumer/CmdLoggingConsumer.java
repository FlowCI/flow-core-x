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

package com.flow.platform.api.consumer;

import com.flow.platform.core.http.converter.RawGsonMessageConverter;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.Logger;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * To handle cmd real time logging from agent
 *
 * @author yang
 */
public class CmdLoggingConsumer extends TextWebSocketHandler {

    private final static Logger LOGGER = new Logger(CmdLoggingConsumer.class);

    private final static int MIN_LENGTH_LOG = 6;

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private RawGsonMessageConverter jsonConverter;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String logItem = message.getPayload();

        if (logItem.length() < MIN_LENGTH_LOG) {
            return;
        }

        // parse log item "category#index#zone#agent#cmdId#content" and send to event "zone:agent"
        int categoryIndex = logItem.indexOf('#', 0);
        String category = logItem.substring(0, categoryIndex);

        int numberIndex = logItem.indexOf('#', categoryIndex + 1);
        String number = logItem.substring(categoryIndex + 1, numberIndex);

        int zoneIndex = logItem.indexOf('#', numberIndex + 1);
        String zone = logItem.substring(numberIndex + 1, zoneIndex);

        int agentIndex = logItem.indexOf('#', zoneIndex + 1);
        String agent = logItem.substring(zoneIndex + 1, agentIndex);

        int cmdIdIndex = logItem.indexOf('#', agentIndex + 1);
        String cmdId = logItem.substring(agentIndex + 1, cmdIdIndex);

        String content = logItem.substring(cmdIdIndex + 1);

        if (category.equals(CmdType.RUN_SHELL.toString())) {
            sendCmdLog(cmdId, content, number);
            return;
        }

        if (category.equals(CmdType.SYSTEM_INFO.toString())) {
            sendAgentSysInfo(content);
        }
    }

    /**
     * send command log
     */
    private void sendCmdLog(String cmdId, String content, String number) {
        String event = String.format("/topic/cmd/%s", cmdId);
        template.convertAndSend(event, "{\"number\": \"" + number + "\", \"content\": \"" + content + "\"}");
    }

    /**
     * send agent sys info
     */
    private void sendAgentSysInfo(String content) {
        Map<String, String> dic = jsonConverter.getGson().fromJson(content, Map.class);
        String event = String.format("/topic/agent/sysinfo/%s/%s", dic.get("zone"), dic.get("name"));
        template.convertAndSend(event, content);
    }
}
