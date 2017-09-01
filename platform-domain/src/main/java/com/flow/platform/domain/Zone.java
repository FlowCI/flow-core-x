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

/**
 * @author gy@fir.im
 */
public class Zone extends Jsonable {

    /**
     * Zone name, unique
     */
    private String name;

    /**
     * Zone zk node path is /{root name}/{zone name}
     */
    private String path;

    /**
     * Cloud provider for manager instance
     */
    private String cloudProvider;

    /**
     * Zone instance image name
     */
    private String imageName;

    /**
     * Minimum idle agent pool size, default is 1
     */
    private Integer minPoolSize = 1;

    /**
     * Maximum idle agent pool size, default is 1
     */
    private Integer maxPoolSize = 1;

    /**
     * Num of instance to start while idle agent not enough
     */
    private Integer numOfStart = minPoolSize;

    /**
     * Agent session timeout in seconds
     */
    private Integer agentSessionTimeout = 600;

    /**
     * Default cmd timeout in seconds if CmdBase.timeout not defined
     */
    private Integer defaultCmdTimeout = 600;

    /**
     * Extra settings for zone
     */
    private Map<String, String> settings = new HashMap<>();

    public Zone() {
    }

    public Zone(String name, String cloudProvider) {
        this.name = name;
        this.cloudProvider = cloudProvider;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCloudProvider() {
        return cloudProvider;
    }

    public void setCloudProvider(String cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public Integer getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(Integer minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public Integer getNumOfStart() {
        return numOfStart;
    }

    public void setNumOfStart(Integer numOfStart) {
        this.numOfStart = numOfStart;
    }

    public Integer getAgentSessionTimeout() {
        return agentSessionTimeout;
    }

    public void setAgentSessionTimeout(Integer agentSessionTimeout) {
        this.agentSessionTimeout = agentSessionTimeout;
    }

    public Integer getDefaultCmdTimeout() {
        return defaultCmdTimeout;
    }

    public void setDefaultCmdTimeout(Integer defaultCmdTimeout) {
        this.defaultCmdTimeout = defaultCmdTimeout;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    public boolean isAvailable() {
        return !Strings.isNullOrEmpty(cloudProvider) && !Strings.isNullOrEmpty(imageName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Zone zone = (Zone) o;

        return name.equals(zone.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Zone{" +
                "name='" + name + '\'' +
                ", cloudProvider='" + cloudProvider + '\'' +
                ", imageName='" + imageName + '\'' +
                ", minPoolSize=" + minPoolSize +
                ", maxPoolSize=" + maxPoolSize +
                ", numOfStart=" + numOfStart +
                "} ";
    }
}
