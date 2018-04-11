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

package com.flow.platform.api.events;

import com.flow.platform.api.domain.job.NodeResultKey;
import com.flow.platform.api.domain.job.NodeStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Node Result status change event
 *
 * @author yang
 */
public class NodeStatusChangeEvent extends ApplicationEvent {

    @Getter
    private final NodeResultKey resultKey;

    @Getter
    private final NodeStatus from;

    @Getter
    private final NodeStatus to;

    public NodeStatusChangeEvent(Object source, NodeResultKey resultKey, NodeStatus from, NodeStatus to) {
        super(source);
        this.resultKey = resultKey;
        this.from = from;
        this.to = to;
    }o
}
