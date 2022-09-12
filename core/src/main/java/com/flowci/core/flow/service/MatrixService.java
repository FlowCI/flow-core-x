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

package com.flowci.core.flow.service;

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.MatrixCounter;
import com.flowci.core.flow.domain.MatrixItem;
import com.flowci.core.flow.domain.MatrixType;
import java.util.List;
import java.util.Map;

/**
 * Statistic Service
 *
 * @author yang
 */
public interface MatrixService {

    /**
     * List system default stats type
     */
    Map<String, MatrixType> defaultTypes();

    /**
     * Get stats type from default and plugins
     */
    List<MatrixType> getStatsType(Flow flow);

        /**
         * List statistic by range
         */
    List<MatrixItem> list(String flowId, String type, int fromDay, int toDay);

    /**
     * Get statistic item
     */
    MatrixItem get(String flowId, String type, int day);

    /**
     * Add statistic item
     */
    MatrixItem add(String flowId, int day, String type, MatrixCounter counter);

}
