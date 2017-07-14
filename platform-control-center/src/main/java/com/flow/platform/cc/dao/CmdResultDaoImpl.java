package com.flow.platform.cc.dao;

import com.flow.platform.cc.dao.adaptor.BaseAdaptor;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.util.ObjectUtil;
import com.google.common.collect.Sets;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by Will on 17/6/13.
 */
@Repository(value = "cmdResultDao")
public class CmdResultDaoImpl extends AbstractBaseDao<String, CmdResult> implements CmdResultDao {

    @Override
    Class getEntityClass() {
        return CmdResult.class;
    }

    @Override
    public List<CmdResult> list(Collection<String> cmdIds) {
        if (cmdIds == null || cmdIds.size() == 0) {
            return new ArrayList<>(0);
        }

        return execute(session -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();

            CriteriaQuery select = builder.createQuery(getEntityClass());
            Root from = select.from(getEntityClass());
            select.where(from.get("cmdId").in(cmdIds));

            return session.createQuery(select).getResultList();
        });
    }

    @Override
    public int updateNotNullOrEmpty(final CmdResult obj) {
        final Map<Field, Object> notNullFields =
            ObjectUtil.findNotNullFieldValue(getEntityClass(),
                obj,
                Sets.newHashSet(Process.class),
                Sets.newHashSet("cmdId"),
                true);

        // all fields are null
        if (notNullFields.isEmpty()) {
            return 0;
        }

        return execute(session -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaUpdate<CmdResult> update = builder.createCriteriaUpdate(CmdResult.class);
            Root<CmdResult> from = update.from(CmdResult.class);

            for (Map.Entry<Field, Object> entry : notNullFields.entrySet()) {
                Field field = entry.getKey();
                Object value = entry.getValue();

                // for cmd result exception type, adaptor will get item of list.. maybe its bug of hibenrate
                if (value instanceof List) {
                    value = BaseAdaptor.GSON.toJson(value);
                }

                update.set(field.getName(), value);
            }

            update.where(builder.equal(from.get("cmdId"), obj.getCmdId()));
            return session.createQuery(update).executeUpdate();
        });
    }
}