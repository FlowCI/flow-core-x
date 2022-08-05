package com.flowci.core.flow.service;

import com.flowci.core.flow.domain.FlowGroup;

import java.util.Collection;
import java.util.List;

public interface FlowGroupService {

    FlowGroup get(String name);

    List<FlowGroup> list(Collection<String> ids);

    FlowGroup create(String name);

    void delete(String name);
}
