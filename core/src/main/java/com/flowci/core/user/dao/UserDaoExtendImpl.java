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

package com.flowci.core.user.dao;

import com.flowci.core.user.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Collection;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

public class UserDaoExtendImpl implements UserDaoExtend {

    @Autowired
    private MongoOperations mongoOps;

    @Override
    public List<User> listUserEmailByIds(Collection<String> ids) {
        Query query = query(where("_id").in(ids));

        query.fields()
                .exclude("_id")
                .exclude("createdAt")
                .exclude("createdBy")
                .exclude("updatedAt")
                .exclude("updatedBy")
                .exclude("role")
                .exclude("passwordOnMd5");

        return mongoOps.find(query, User.class);
    }
}
