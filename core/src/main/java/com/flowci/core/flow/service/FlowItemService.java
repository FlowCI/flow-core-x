package com.flowci.core.flow.service;

import com.flowci.core.flow.domain.FlowItem;

import java.util.List;

/**
 * The service used to list all flow items that available for current user
 */
public interface FlowItemService {

    List<FlowItem> list();

    boolean existed(String name);
}
