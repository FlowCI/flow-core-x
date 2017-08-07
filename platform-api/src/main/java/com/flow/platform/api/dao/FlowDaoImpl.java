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

import com.flow.platform.api.domain.Flow;
import com.flow.platform.core.dao.AbstractBaseDao;
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
@Repository(value = "flowDao")
public class FlowDaoImpl extends AbstractBaseDao<String, Flow> implements FlowDao {

    @Override
    protected Class<Flow> getEntityClass() {
        return Flow.class;
    }

    @Override
    protected String getKeyName() {
        return "path";
    }

    @Override
    public List<Flow> list() {
        return execute((Session session) -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Flow> select = builder.createQuery(Flow.class);
            Root<Flow> flow = select.from(Flow.class);
            Predicate condition = builder.not(flow.get("path").isNull());
            select.where(condition);
            return session.createQuery(select).list();
        });
    }
}
