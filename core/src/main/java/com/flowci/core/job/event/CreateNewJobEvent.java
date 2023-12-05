/*
 * Copyright 2018 flow.ci
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

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.FlowYml;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.common.domain.StringVars;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Start job from jobRunExecutor
 *
 * @author yang
 */
@Getter
public class CreateNewJobEvent extends ApplicationEvent {

    private final Flow flow;

    private final FlowYml ymlEntity;

    private final Trigger trigger;

    private final StringVars input;

    public CreateNewJobEvent(Object source, Flow flow, FlowYml ymlEntity, Trigger trigger, StringVars input) {
        super(source);
        this.flow = flow;
        this.ymlEntity = ymlEntity;
        this.trigger = trigger;
        this.input = input;
    }
}
