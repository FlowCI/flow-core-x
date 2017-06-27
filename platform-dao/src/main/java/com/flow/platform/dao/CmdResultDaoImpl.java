package com.flow.platform.dao;

import com.flow.platform.domain.Agent;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.util.ObjectUtil;
import com.google.common.collect.Sets;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

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
    public int updateNotNull(final CmdResult obj) {
        final Map<Field, Object> notNullFields =
                ObjectUtil.findNotNullFieldValue(getEntityClass(),
                        obj,
                        Sets.newHashSet(Process.class),
                        Sets.newHashSet("cmdId"));

        // all fields are null
        if (notNullFields.isEmpty()) {
            return 0;
        }

        Session session = getSession();
        CriteriaBuilder builder = session.getCriteriaBuilder();

        CriteriaUpdate<CmdResult> update = builder.createCriteriaUpdate(CmdResult.class);


        Root<CmdResult> from = update.from(CmdResult.class);

        for (Map.Entry<Field, Object> entry : notNullFields.entrySet()) {
            Field field = entry.getKey();
            Object value = entry.getValue();
            update.set(field.getName(), value);
        }

        update.where(builder.equal(from.get("cmdId"), obj.getCmdId()));

        return session.createQuery(update).executeUpdate();
    }
}
