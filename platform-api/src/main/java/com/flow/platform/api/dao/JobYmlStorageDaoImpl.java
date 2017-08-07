/*
 * Copyright 2017 flow.ci
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
package com.flow.platform.api.dao;

import com.flow.platform.api.domain.JobYmlStorage;
import com.flow.platform.api.domain.YmlStorage;
import java.math.BigInteger;
import org.springframework.stereotype.Repository;

/**
 * @author lhl
 */

@Repository(value = "jobYmlStorageDao")
public class JobYmlStorageDaoImpl extends AbstractBaseDao<BigInteger, JobYmlStorage> implements JobYmlStorageDao {

    @Override
    Class<JobYmlStorage> getEntityClass() {
        return JobYmlStorage.class;
    }

    @Override
    String getKeyName() {
        return "jobId";
    }
}
