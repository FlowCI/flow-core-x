package com.flow.platform.dao;

import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.util.DateUtil;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Will on 17/6/13.
 */
@Repository(value = "cmdDao")
public class CmdDaoImpl extends AbstractBaseDao<String, Cmd> implements CmdDao {

    @Override
    Class getEntityClass() {
        return Cmd.class;
    }

    @Override
    public void update(Cmd obj) {
        execute(session -> {
            obj.setUpdatedDate(DateUtil.now());
            session.update(obj);
            return null;
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
