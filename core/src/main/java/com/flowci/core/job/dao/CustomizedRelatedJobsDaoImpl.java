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

package com.flowci.core.job.dao;

import com.flowci.core.job.domain.RelatedJobs;
import com.flowci.core.job.domain.JobDesc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class CustomizedRelatedJobsDaoImpl implements CustomizedRelatedJobsDao {

    private final static String FieldGitEventId = "gitEventId";

    private final static String FieldJobs = "jobs";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void addRelatedInfo(String gitEventId, JobDesc job) {
        mongoTemplate.upsert(
                new Query().addCriteria(Criteria.where(FieldGitEventId).is(gitEventId)),
                new Update().push(FieldJobs, job),
                RelatedJobs.class);
    }
}
