package com.flow.platform.dao;

import com.flow.platform.domain.CmdResult;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.stereotype.Repository;

/**
 * Created by Will on 17/6/13.
 */
@Repository
public class CmdResultDaoImpl extends AbstractBaseDao<String, CmdResult> implements CmdResultDao {

    @Override
    Class getEntityClass() {
        return CmdResult.class;
    }

    @Override
    public CmdResult findByCmdId(String cmdId) {
        return (CmdResult) execute(session -> getSession().createQuery("from CmdResult where CMD_ID = :cmdId")
                .setParameter("cmdId", cmdId)
                .uniqueResult());
    }

    @Override
    public void baseDelete(String condition) {
        try (Session session = getSession()) {
            Transaction tx = session.beginTransaction();
            session.createQuery("delete CmdResult where ".concat(condition)).executeUpdate();
            tx.commit();
        }
    }
}
