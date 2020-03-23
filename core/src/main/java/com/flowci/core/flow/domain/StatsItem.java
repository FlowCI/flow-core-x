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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author yang
 */
@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode(of = "id")
@Document(collection = "flow_stats")
@CompoundIndexes(
    @CompoundIndex(name = "index_flow_day_type", def = "{'flowId' : 1, 'day': -1, 'type': 1}")
)
public class StatsItem {

    // zero day used for total
    public static final int ZERO_DAY = 0;

    @Id
    private String id; // auto id

    private String flowId;

    /**
     * Int value to represent day, ex 20190123
     */
    private int day;

    /**
     * Status Type, ex: status, ut, code coverage
     */
    private String type;

    /**
     * Num of stats item counted today
     */
    private int numOfToday;

    /**
     * Num of stats item counted in total
     */
    private int numOfTotal;

    /**
     * Current day counter
     */
    private StatsCounter counter = new StatsCounter();

    /**
     * Counter in total
     */
    private StatsCounter total = new StatsCounter();

    public void plusOneToday() {
        numOfToday++;
    }

    public void plusDayCounter(StatsCounter other) {
        counter.add(other);
    }
}
