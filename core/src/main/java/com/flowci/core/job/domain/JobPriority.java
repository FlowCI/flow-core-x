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

package com.flowci.core.job.domain;

import com.flowci.core.common.domain.Mongoable;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Record all RUNNING jobs (flow, build number) for selector
 * To prioritize which job has higher priority for agent dispatching
 */
@Getter
@Setter
@Document(collection = "job_priority")
public class JobPriority extends Mongoable {

    @NonNull
    @Indexed(unique = true)
    private String flowId;

    // ongoing job build number that received from queue into application
    private List<Long> queue = new ArrayList<>();
}
