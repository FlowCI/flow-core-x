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

package com.flow.platform.cc.util;

import com.flow.platform.domain.AgentPath;
import javax.annotation.PostConstruct;
import org.apache.curator.utils.ZKPaths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author gy@fir.im
 */
@Component
public class ZKHelper {

    private static String ROOT_NODE;

    @Value("${zk.node.root}")
    private String rootNodeName;

    @PostConstruct
    public void init() {
        ROOT_NODE = rootNodeName;
    }

    /**
     * Get zk path builder for agent
     *
     * @param zone zone name (nullable)
     * @param name agent name (nullable)
     * @return zone path as string
     */
    public static String buildPath(String zone, String name) {
        return ZKPaths.makePath(ROOT_NODE, zone, name);
    }

    public static String buildPath(AgentPath agentPath) {
        return ZKPaths.makePath(ROOT_NODE, agentPath.getZone(), agentPath.getName());
    }

    public static String getNameFromPath(String path) {
        return ZKPaths.getNodeFromPath(path);
    }
}
