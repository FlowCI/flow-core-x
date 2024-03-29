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

package com.flowci.core.job.event;

import com.flowci.core.agent.domain.TtyCmd;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TtyStatusUpdateEvent extends ApplicationEvent {

    private final TtyCmd.Out out;

    public TtyStatusUpdateEvent(Object source, TtyCmd.Out out) {
        super(source);
        this.out = out;
    }
}
