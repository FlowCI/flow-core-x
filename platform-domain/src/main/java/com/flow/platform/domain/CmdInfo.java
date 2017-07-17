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

package com.flow.platform.domain;

/**
 * Used for send cmd info
 *
 * @author gy@fir.im
 */
public class CmdInfo extends CmdBase {

    public CmdInfo(String zone, String agent, CmdType type, String cmd) {
        super(zone, agent, type, cmd);
    }

    public CmdInfo(AgentPath agentPath, CmdType type, String cmd) {
        super(agentPath, type, cmd);
    }
}
