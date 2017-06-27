package com.flow.platform.dao;

import com.flow.platform.domain.CmdResult;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    public CmdResult findByCmdId(String cmdId) {
        return (CmdResult) execute(session -> getSession().createQuery("from CmdResult where CMD_ID = :cmdId")
                .setParameter("cmdId", cmdId)
                .uniqueResult());
    }

    @Override
    public void baseDelete(String condition) {
        getSession().createQuery("delete from CmdResult").executeUpdate();
    }
}
