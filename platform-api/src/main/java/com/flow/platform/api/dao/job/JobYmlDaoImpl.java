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
package com.flow.platform.api.dao.job;
import com.flow.platform.api.domain.job.JobYml;
import com.flow.platform.core.dao.AbstractBaseDao;
import java.math.BigInteger;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

/**
 * @author lhl
 */

@Repository(value = "jobYmlDao")
public class JobYmlDaoImpl extends AbstractBaseDao<BigInteger, JobYml> implements JobYmlDao {

    @Override
    protected Class<JobYml> getEntityClass() {
        return JobYml.class;
    }

    @Override
    protected String getKeyName() {
        return "jobId";
    }

    @Override
    public void delete(List<BigInteger> jobIds) {
        execute((Session session) -> {
            String delete = String.format("delete from JobYml where job_id in (:list)");
            Query query = session.createQuery(delete);
            query.setParameterList("list", jobIds);
            int affectedRows = query.executeUpdate();
            if (affectedRows == jobIds.size()) {
                return true;
            }
            return false;
        });
    }
}
