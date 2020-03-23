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

package com.flowci.core.flow.dao;

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Flow.Status;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * @author yang
 */
@Repository
public interface FlowDao extends MongoRepository<Flow, String> {

    Flow findByName(String name);

    List<Flow> findAllByStatusAndCreatedBy(Status status, String createdBy);

    List<Flow> findAllByIdInAndStatus(Iterable<String> id, Status status);
}
