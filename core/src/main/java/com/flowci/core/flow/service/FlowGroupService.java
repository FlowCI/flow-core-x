package com.flowci.core.flow.service;

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.FlowGroup;

import java.util.Collection;
import java.util.List;

public interface FlowGroupService {

    FlowGroup get(String name);

    List<FlowGroup> list(Collection<String> ids);

    List<Flow> flows(String id);

    FlowGroup create(String name);

    /**
     * Add flow to a group
     */
    void addToGroup(String flowName, String groupName);

    /**
     * Remove flow from a group
     */
    void removeFromGroup(String flowName);

    void delete(String name);
}
