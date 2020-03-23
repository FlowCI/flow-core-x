/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.credential.dao;

import com.flowci.core.credential.domain.Credential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

public class CustomCredentialDaoImpl implements CustomCredentialDao {

    @Autowired
    private MongoOperations operations;

    @Override
    public List<Credential> listNameOnly() {
        Query query = Query.query(new Criteria()).with(new Sort(Sort.Direction.ASC, "createdAt"));
        query.fields().include("name");

        return operations.find(query, Credential.class);
    }

    @Override
    public List<Credential> listNameOnly(Credential.Category category) {
        Criteria criteria = Criteria.where("category").is(category);
        Query query = Query.query(criteria).with(new Sort(Sort.Direction.ASC, "createdAt"));
        query.fields().include("name");
        return operations.find(query, Credential.class);
    }
}
