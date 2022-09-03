package com.flowci.core.flow.service;

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.FlowGroup;

import java.util.List;

public interface FlowGroupService {

    FlowGroup get(String name);

    List<Flow> flows(String id);

    FlowGroup create(String name);

    /**
     * Add flow to a group
     */
    void addToGroup(String flowName, String groupName);

    void delete(String name);
}
