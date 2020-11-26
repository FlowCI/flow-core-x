package com.flowci.core.flow.dao;

import com.flowci.core.flow.domain.Yml;

import java.util.List;

public interface YmlCustomDao {

    /**
     * List all Yml instance without raw yaml string
     */
    List<Yml> findAllByFlowId(String flowId);

}
