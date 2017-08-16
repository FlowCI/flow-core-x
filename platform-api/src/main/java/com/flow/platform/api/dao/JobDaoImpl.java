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

import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.NodeStatus;
import com.flow.platform.core.dao.AbstractBaseDao;
import java.math.BigInteger;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

/**
 * @author yh@firim
 */
@Repository(value = "jobDao")
public class JobDaoImpl extends AbstractBaseDao<BigInteger, Job> implements JobDao {

    @Override
    protected Class<Job> getEntityClass() {
        return Job.class;
    }

    @Override
    protected String getKeyName() {
        return "id";
    }

    @Override
    public List<Job> list(NodeStatus... statuses) {
        return execute((Session session) -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Job> select = builder.createQuery(Job.class);
            Root<Job> job = select.from(Job.class);
            Predicate condition = job.get("status").in(statuses);
            select.where(condition);
            return session.createQuery(select).list();
        });
    }

    @Override
    public List<Job> list(List<String> sessionIds, NodeStatus nodeStatus) {
        return execute((Session session) -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Job> select = builder.createQuery(Job.class);
            Root<Job> job = select.from(Job.class);
            Predicate aCondition = builder.equal(job.get("status"), nodeStatus);
            Predicate bCondition = job.get("sessionId").in(sessionIds);
            select.where(builder.and(aCondition, bCondition));
            return session.createQuery(select).list();
        });
    }

    @Override
    public List<Job> listLatest(List<String> nodeNames) {
        return execute((Session session) -> {
            String string = String.join("','", nodeNames);
            string = "'" + string + "'";

            String select = String
                .format("from Job where id in (select max(id) from Job where nodeName in ( %s ) group by nodePath)",
                    string);
            List<Job> jobs = (List<Job>) session.createQuery(select)
                .list();
            return jobs;
        });
    }


    @Override
    public List<Job> list(String nodePath) {
        return execute((Session session) -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Job> select = builder.createQuery(Job.class);
            Root<Job> job = select.from(Job.class);
            Predicate condition = job.get("nodeName").in(nodePath);
            select.where(condition);
            select.orderBy(builder.desc(job.get("number")));
            return session.createQuery(select).list();
        });
    }

    @Override
    public Job get(String flowName, Integer number) {
        return execute((Session session) -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Job> select = builder.createQuery(Job.class);
            Root<Job> job = select.from(Job.class);
            Predicate condition = builder.equal(job.get("nodeName"), flowName);
            Predicate bCondition = builder.equal(job.get("number"), number);
            select.where(builder.and(condition, bCondition));
            return session.createQuery(select).uniqueResult();
        });
    }

    @Override
    public Integer maxBuildNumber(String flowName) {
        return execute((Session session) -> {
            String select = String.format("select max(number) from Job where node_name='%s'", flowName);
            Integer integer = (Integer) session.createQuery(select).uniqueResult();
            if (integer == null) {
                integer = 0;
            }
            return integer;
        });
    }
}
