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

package com.flow.platform.cc.dao;

import com.flow.platform.core.dao.AbstractBaseDao;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.DateUtil;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.*;
import java.util.List;
import java.util.Set;

/**
 * @author Will
 */
@Repository(value = "cmdDao")
public class CmdDaoImpl extends AbstractBaseDao<String, Cmd> implements CmdDao {

    @Override
    protected Class getEntityClass() {
        return Cmd.class;
    }

    @Override
    protected String getKeyName() {
        return "id";
    }

    @Override
    public List<Cmd> list(String sessionId) {
        return execute(session -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery select = builder.createQuery(getEntityClass());

            Root<Cmd> root = select.from(getEntityClass());
            select.where(builder.equal(root.get("sessionId"), sessionId));

            return session.createQuery(select).getResultList();
        });
    }

    @Override
    public List<Cmd> list(AgentPath agentPath, Set<CmdType> types, Set<CmdStatus> status) {
        return execute(session -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery select = builder.createQuery(getEntityClass());

            // select from
            Root<Cmd> root = select.from(getEntityClass());

            // where
            Predicate where = buildAgentPathPredicate(builder, root.get("agentPath"), agentPath);

            Predicate typesPredicate = buildInPredicate(builder, root.get("type"), types);
            if (where != null && typesPredicate != null) {
                where = builder.and(where, typesPredicate);
            }

            Predicate statusPredicate = buildInPredicate(builder, root.get("status"), status);
            if (where != null && statusPredicate != null) {
                where = builder.and(where, statusPredicate);
            }

            if (where != null) {
                select.where(where);
            }

            // order by
            select.orderBy(builder.asc(root.get("createdDate")));

            // execute
            return session.createQuery(select).getResultList();
        });
    }

    private Predicate buildAgentPathPredicate(
        CriteriaBuilder builder, Path<?> path, AgentPath agentPath) {
        Predicate predicate = null;
        if (agentPath != null) {
            String zoneName = agentPath.getZone();
            String agentName = agentPath.getName();

            // set zone name
            if (zoneName != null) {
                predicate = builder.equal(path.get("zone"), zoneName);

                // set agent name
                if (agentName != null) {
                    predicate = builder.and(predicate, builder.equal(path.get("name"), agentName));
                }
            }
        }
        return predicate;
    }

    private <T> Predicate buildInPredicate(CriteriaBuilder builder, Path<?> path, Set<T> sets) {
        Predicate predicate = null;
        if (sets != null && sets.size() > 0) {
            predicate = path.in(sets);
        }
        return predicate;
    }
}
