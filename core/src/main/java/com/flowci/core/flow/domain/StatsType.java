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

package com.flowci.core.flow.domain;

import java.util.LinkedList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@EqualsAndHashCode(of = "name")
@Accessors(chain = true)
public class StatsType {

    public static final String JOB_STATUS = "default/ci_job_status";

    private String name;

    private String desc;

    /**
     * data is percentage based or not
     */
    private boolean percent;

    // stats fields that applied in counter as key
    private List<String> fields = new LinkedList<>();

    public StatsItem createEmptyItem() {
        StatsCounter counter = new StatsCounter();
        for (String field : fields) {
            counter.put(field, 0.0F);
        }

        return new StatsItem()
            .setType(name)
            .setCounter(counter);
    }
}
