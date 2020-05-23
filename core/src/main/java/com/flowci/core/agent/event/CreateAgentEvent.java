/*
 * Copyright 2020 flow.ci
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

package com.flowci.core.agent.event;

import com.flowci.core.common.event.SyncEvent;
import com.flowci.domain.Agent;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.Set;

@Getter
@Setter
public class CreateAgentEvent extends ApplicationEvent implements SyncEvent {

    private final String name;

    private final Set<String> tags;

    private final String hostId;

    private Agent created;

    public CreateAgentEvent(Object source, String name, Set<String> tags, String hostId) {
        super(source);
        this.name = name;
        this.tags = tags;
        this.hostId = hostId;
    }
}
