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

package com.flowci.core.job.domain;

import com.flowci.common.sm.Context;
import com.flowci.zookeeper.InterLock;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class JobSmContext extends Context {

    private final String jobId;

    private Job job;

    private JobYml yml;

    private Step step;

    private InterLock lock;

    public Job.Status getTargetToJobStatus() {
        String name = this.to.getName();
        return Job.Status.valueOf(name);
    }

    public boolean hasLock() {
        return lock != null;
    }
}
