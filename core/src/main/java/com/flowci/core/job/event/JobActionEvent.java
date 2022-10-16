/*
 * Copyright 2021 flow.ci
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

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class JobActionEvent extends ApplicationEvent {

    public static final String ACTION_TO_RUN = "toRun";

    public static final String ACTION_TO_TIMEOUT = "toTimeout";

    private final String jobId;

    private final String action;

    public JobActionEvent(Object source, String jobId, String action) {
        super(source);
        this.jobId = jobId;
        this.action = action;
    }

    public boolean isToRun() {
        return this.action.equals(ACTION_TO_RUN);
    }

    public boolean isToTimeOut() {
        return this.action.equals(ACTION_TO_TIMEOUT);
    }
}
