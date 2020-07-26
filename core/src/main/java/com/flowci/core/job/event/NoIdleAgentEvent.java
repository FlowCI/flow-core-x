/*
 * Copyright 2020 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.flowci.core.job.event;

import com.flowci.core.common.event.AsyncEvent;
import com.flowci.tree.Selector;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NoIdleAgentEvent extends ApplicationEvent implements AsyncEvent {

    private final String jobId;

    private final Selector selector;

    public NoIdleAgentEvent(Object source, String jobId, Selector selector) {
        super(source);
        this.jobId = jobId;
        this.selector = selector;
    }
}
