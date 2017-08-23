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
import com.flow.platform.api.domain.NodeResult;
import com.flow.platform.api.domain.NodeResultKey;
import com.flow.platform.api.domain.NodeStatus;
import com.flow.platform.api.domain.NodeTag;
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
 * @author lhl
 */
@Repository(value = "nodeResultDao")

public class NodeResultDaoImpl extends AbstractBaseDao<NodeResultKey, NodeResult> implements NodeResultDao {

    @Override
    protected Class<NodeResult> getEntityClass() {
        return NodeResult.class;
    }

    @Override
    protected String getKeyName() {
        return "nodeResultKey";
    }

    @Override
    public NodeResult get(BigInteger jobId, NodeStatus status, NodeTag tag) {
        return execute((Session session) -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<NodeResult> select = builder.createQuery(NodeResult.class);
            Root<NodeResult> nodeResultRoot = select.from(NodeResult.class);
            Predicate aCondition = builder.equal(nodeResultRoot.get("nodeResultKey").get("jobId"), jobId);
            Predicate bCondition = builder.equal(nodeResultRoot.get("status"), status);
            Predicate cCondition = builder.equal(nodeResultRoot.get("nodeTag"), tag);
            select.where(builder.and(aCondition, bCondition, cCondition));
            return session.createQuery(select).uniqueResult();
        });
    }

    @Override
    public List<NodeResult> list(Job job) {
        return execute((Session session) -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<NodeResult> select = builder.createQuery(NodeResult.class);
            Root<NodeResult> nodeResultRoot = select.from(NodeResult.class);
            Predicate aCondition = builder.equal(nodeResultRoot.get("nodeResultKey").get("jobId"), job.getId());
            select.where(aCondition);
            select.orderBy(builder.desc(nodeResultRoot.get("createdAt")));
            return session.createQuery(select).list();
        });
    }
}