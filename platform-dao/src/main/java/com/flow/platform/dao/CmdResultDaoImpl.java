package com.flow.platform.dao;

import com.flow.platform.domain.CmdResult;

/**
 * Created by Will on 17/6/13.
 */
public class CmdResultDaoImpl extends DaoBase implements CmdResultDao {
    @Override
    public CmdResult findByCmdId(String cmdId) {
        CmdResult cmdResult = (CmdResult)getSession().createQuery("from CmdResult where CMD_ID = :cmdId")
                .setParameter("cmdId", cmdId)
                .uniqueResult();
        return cmdResult;
    }
}
