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

package com.flowci.core.flow.event;

import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author yang
 */
@Getter
public class GitTestEvent extends ApplicationEvent {

    public enum Status {

        FETCHING,

        DONE,

        ERROR
    }

    private final String flowId;

    private final List<String> branches = new LinkedList<>();

    private final Status status;

    private final String error;

    public GitTestEvent(Object source, String flowId) {
        super(source);
        this.flowId = flowId;
        this.status = Status.FETCHING;
        this.error = null;
    }

    public GitTestEvent(Object source, String flowId, List<String> branches) {
        super(source);
        this.flowId = flowId;
        this.branches.addAll(branches);
        this.status = Status.DONE;
        this.error = null;
    }

    public GitTestEvent(Object source, String flowId, String error) {
        super(source);
        this.flowId = flowId;
        this.error = error;
        this.status = Status.ERROR;
    }
}
