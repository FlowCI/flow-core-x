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

import com.google.common.base.Strings;

import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author gy@fir.im
 */
@NoArgsConstructor
@EqualsAndHashCode(of = {"name"}, callSuper = false)
@ToString(of = {"name", "cloudProvider", "imageName", "minPoolSize", "maxPoolSize", "numOfStart"})
public class Zone extends Jsonable {

    /**
     * Zone name, unique
     */
    @Getter
    @Setter
    private String name;

    /**
     * Zone zk node path is /{root name}/{zone name}
     */
    @Getter
    @Setter
    private String path;

    /**
     * Cloud provider for manager instance
     */
    @Getter
    @Setter
    private String cloudProvider;

    /**
     * Zone instance image name
     */
    @Getter
    @Setter
    private String imageName;

    /**
     * Minimum idle agent pool size, default is 1
     */
    @Getter
    @Setter
    private Integer minPoolSize = 1;

    /**
     * Maximum idle agent pool size, default is 1
     */
    @Getter
    @Setter
    private Integer maxPoolSize = 1;

    /**
     * Num of instance to start while idle agent not enough
     */
    @Getter
    @Setter
    private Integer numOfStart = minPoolSize;

    /**
     * Agent session timeout in seconds
     */
    @Getter
    @Setter
    private Integer agentSessionTimeout = 600;

    /**
     * Default cmd timeout in seconds if CmdBase.timeout not defined
     */
    @Getter
    @Setter
    private Integer defaultCmdTimeout = 600;

    /**
     * Extra settings for zone
     */
    @Getter
    private final Map<String, String> settings = new HashMap<>();

    public Zone(String name, String cloudProvider) {
        this.name = name;
        this.cloudProvider = cloudProvider;
    }

    public boolean isAvailable() {
        return !Strings.isNullOrEmpty(cloudProvider) && !Strings.isNullOrEmpty(imageName);
    }
}
