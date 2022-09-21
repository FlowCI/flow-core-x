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

package com.flowci.core.flow.dao;

import com.flowci.core.flow.domain.MatrixItem;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author yang
 */
@Repository
public interface MatrixItemDao extends MongoRepository<MatrixItem, String> {

    @Query("{'flowId':?0, 'type': ?1, 'day' : {$gte : ?2, $lte : ?3}}")
    List<MatrixItem> findByFlowIdAndTypeDayBetween(String flowId, String type, int dayGT, int dayLT, Sort sort);

    @Query("{'flowId':?0, 'day' : {$gte : ?1, $lte : ?2}}")
    List<MatrixItem> findByFlowIdDayBetween(String flowId, int dayGT, int dayLT, Sort sort);

    Optional<MatrixItem> findByFlowIdAndDayAndType(String flowId, int day, String type);

    List<MatrixItem> findAllByFlowIdInAndDayAndType(Collection<String> flowIds, int day, String type);

    void deleteByFlowId(String flowId);
}
