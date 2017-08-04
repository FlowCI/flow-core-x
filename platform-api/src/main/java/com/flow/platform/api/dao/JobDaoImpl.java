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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.springframework.stereotype.Repository;

/**
 * @author yh@firim
 */
@Repository(value = "jobDao")
public class JobDaoImpl extends AbstractBaseDao<BigInteger, Job> implements JobDao {

    @Override
    Class<Job> getEntityClass() {
        return Job.class;
    }

    @Override
    String getKeyName() {
        return "id";
    }

    @Override
    public List<Job> list() {
        return execute((Session session) -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Job> select = builder.createQuery(Job.class);
            Root<Job> job = select.from(Job.class);
            Predicate condition = builder.not(job.get("id").isNull());
            select.where(condition);
            return session.createQuery(select).list();
        });
    }

    @Override
    public List<Job> list(NodeStatus... statuses) {
        return execute((Session session) -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Job> select = builder.createQuery(Job.class);
            Root<Job> job = select.from(Job.class);
            Set<NodeStatus> nodeStatuses = new HashSet<>();
            for (NodeStatus status : statuses) {
                nodeStatuses.add(status);
            }
            Predicate condition = job.get("status").in(nodeStatuses);
            select.where(condition);
            return session.createQuery(select).list();
        });
    }


    @Override
    public List<Job> listLatest(List<String> names) {
        return execute((Session session) -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Job> select = builder.createQuery(Job.class);
            Root<Job> job = select.from(Job.class);
            Set<String> strings = new HashSet<>();
            for (String name : names) {
                strings.add(name);
            }
            Predicate condition = job.get("nodeName").in(names);
            select.where(condition);
            select.orderBy(builder.asc(job.get("createdAt")));
            List<Job> originJobs = session.createQuery(select)
                .list();
            List<Job> jobs = new ArrayList<>();
            for (String name : names){
                Job j = findByNodeName(name, originJobs);
                if(j != null){
                    jobs.add(j);
                }
            }
            return jobs;
        });
    }

    private Job findByNodeName(String nodeName, List<Job> jobs){
        Job j = null;
        for (Job job : jobs) {
            if(job.getNodeName().equals(nodeName)){
                j = job;
                break;
            }
        }
        return j;
    }
}
