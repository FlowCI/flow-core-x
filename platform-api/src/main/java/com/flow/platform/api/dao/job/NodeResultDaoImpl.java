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

import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeResultKey;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.job.NodeTag;
import com.flow.platform.core.dao.AbstractBaseDao;
import java.math.BigInteger;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
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
        return "key";
    }

    @Override
    public NodeResult get(BigInteger jobId, NodeStatus status, NodeTag tag) {
        return execute(session -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<NodeResult> select = builder.createQuery(NodeResult.class);
            Root<NodeResult> nodeResultRoot = select.from(NodeResult.class);
            Predicate aCondition = builder.equal(nodeResultRoot.get("key").get("jobId"), jobId);
            Predicate bCondition = builder.equal(nodeResultRoot.get("status"), status);
            Predicate cCondition = builder.equal(nodeResultRoot.get("nodeTag"), tag);
            select.where(builder.and(aCondition, bCondition, cCondition));
            return session.createQuery(select).uniqueResult();
        });
    }

    @Override
    public List<NodeResult> list(BigInteger jobId) {
        return execute(session -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<NodeResult> select = builder.createQuery(NodeResult.class);
            Root<NodeResult> nodeResultRoot = select.from(NodeResult.class);
            Predicate aCondition = builder.equal(nodeResultRoot.get("key").get("jobId"), jobId);
            select.where(aCondition);
            select.orderBy(builder.desc(nodeResultRoot.get("createdAt")));
            return session.createQuery(select).list();
        });
    }

    @Override
    public int update(BigInteger jobId, NodeStatus target) {
        return execute(session -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaUpdate<NodeResult> update = builder.createCriteriaUpdate(NodeResult.class);
            Root<NodeResult> root = update.getRoot();

            update.set(root.get("status"), target);
            update.where(builder.equal(root.get("key").get("jobId"), jobId));

            return session.createQuery(update).executeUpdate();
        });
    }
}