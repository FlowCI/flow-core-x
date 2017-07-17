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

package com.flow.platform.cloud;

import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Instance;
import com.flow.platform.domain.Zone;

import java.util.Collection;
import java.util.List;

/**
 * @author yang
 */
public interface InstanceManager {

    /**
     * Create unique instance name
     *
     * @return Instance name
     */
    String instanceName();

    /**
     * Find instance by unique name
     *
     * @param name instance unique name
     * @return instance object
     */
    Instance find(String name);

    /**
     * Find instance by agent path
     */
    Instance find(AgentPath agentPath);

    /**
     * Get all instance list
     *
     * @return instance list
     */
    Collection<Instance> instances();

    /**
     * Async to start instance
     *
     * @return List of instance name
     */
    List<String> batchStartInstance(Zone zone);

    /**
     * Add instance to clean list
     */
    void addToCleanList(Instance instance);

    /**
     * Load instance from provider, check alive duration is over the target and status
     *
     * @param maxAliveDuration target alive duration in seconds
     * @param status instance status
     */
    void cleanFromProvider(long maxAliveDuration, String status);

    /**
     * Delete all instance and cleanAll the cloud
     */
    void cleanAll();

    /**
     * Periodically check and delete cleanAll up instance list
     */
    void cleanInstanceTask();
}
