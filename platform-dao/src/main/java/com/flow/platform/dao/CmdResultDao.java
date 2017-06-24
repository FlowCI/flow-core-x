package com.flow.platform.dao;

import com.flow.platform.domain.CmdResult;

/**
 * Created by Will on 17/6/13.
 */
public interface CmdResultDao extends BaseDao<String, CmdResult> {

    CmdResult findByCmdId(String cmdId);

    void baseDelete(String condition);
}
