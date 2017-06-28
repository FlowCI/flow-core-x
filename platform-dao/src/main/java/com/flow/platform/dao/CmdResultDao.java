package com.flow.platform.dao;

import com.flow.platform.domain.CmdResult;

import java.util.Collection;
import java.util.List;

/**
 * Created by Will on 17/6/13.
 */
public interface CmdResultDao extends BaseDao<String, CmdResult> {

    /**
     * List cmd result by ids
     *
     * @param cmdIds
     * @return
     */
    List<CmdResult> list(Collection<String> cmdIds);

    /**
     * Only update not null fields or empty collection
     *
     * @param obj
     * @return
     */
    int updateNotNullOrEmpty(CmdResult obj);
}
