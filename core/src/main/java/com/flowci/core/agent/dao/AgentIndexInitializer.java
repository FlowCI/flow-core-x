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

package com.flowci.core.agent.dao;

import com.flowci.core.agent.domain.Agent;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Component
public class AgentIndexInitializer {

    @Autowired
    protected MongoOperations mongoOps;

    @PostConstruct
    public void createIndexOnName() {
        mongoOps.indexOps(Agent.class)
            .ensureIndex(new Index().on("name", Direction.ASC).unique());
    }

    @PostConstruct
    public void createIndexOnToken() {
        mongoOps.indexOps(Agent.class)
            .ensureIndex(new Index().on("token", Direction.ASC).unique());
    }
}
