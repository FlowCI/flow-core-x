/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.flow.service;

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.UpdateYAMLSource;
import com.flowci.core.flow.domain.WebhookStatus;
import com.flowci.domain.VarValue;

import java.util.List;
import java.util.Map;

/**
 * @author yang
 */
public interface FlowSettingService {

    void rename(Flow flow, String newName);

    /**
     * Set flow YAML source
     */
    void set(Flow flow, UpdateYAMLSource source);

    /**
     * Set flow webhook status
     */
    void set(Flow flow, WebhookStatus ws);

    /**
     * Add vars to flow locally
     */
    void add(Flow flow, Map<String, VarValue> vars);

    /**
     * Remove vars from flow locally
     */
    void remove(Flow flow, List<String> vars);
}
