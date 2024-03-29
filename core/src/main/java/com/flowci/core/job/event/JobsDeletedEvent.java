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

package com.flowci.core.job.event;

import com.flowci.core.flow.domain.Flow;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author yang
 */
@Getter
public class JobsDeletedEvent extends ApplicationEvent {

    private final Flow flow;

    private final Long numOfJobs;

    public JobsDeletedEvent(Object source, Flow flow, Long numOfJobs) {
        super(source);
        this.flow = flow;
        this.numOfJobs = numOfJobs;
    }
}
