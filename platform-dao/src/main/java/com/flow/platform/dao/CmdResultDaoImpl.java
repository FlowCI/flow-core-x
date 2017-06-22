package com.flow.platform.dao;

import com.flow.platform.domain.CmdResult;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * Created by Will on 17/6/13.
 */
public class CmdResultDaoImpl extends DaoBase implements CmdResultDao {
    @Override
    public CmdResult findByCmdId(String cmdId) {
        CmdResult cmdResult = execute(session -> (CmdResult)getSession().createQuery("from CmdResult where CMD_ID = :cmdId")
                .setParameter("cmdId", cmdId)
                .uniqueResult());
        return cmdResult;
    }

    @Override
    public void baseDelete(String condition) {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        session.createQuery("delete CmdResult where ".concat(condition)).executeUpdate();
        tx.commit();
    }
}
