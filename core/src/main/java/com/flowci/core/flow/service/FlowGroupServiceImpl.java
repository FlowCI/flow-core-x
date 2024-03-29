package com.flowci.core.flow.service;

import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.FlowGroupDao;
import com.flowci.core.flow.dao.FlowUsersDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.FlowGroup;
import com.flowci.core.flow.domain.FlowItem;
import com.flowci.common.exception.ArgumentException;
import com.flowci.common.exception.DuplicateException;
import com.flowci.common.exception.NotFoundException;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class FlowGroupServiceImpl implements FlowGroupService {

    private final FlowDao flowDao;

    private final FlowGroupDao flowGroupDao;

    private final FlowUsersDao flowUsersDao;

    private final SessionManager sessionManager;

    @Override
    public FlowGroup get(String name) {
        Optional<FlowGroup> optional = flowGroupDao.findByName(name);
        if (optional.isEmpty()) {
            throw new NotFoundException("Group {0} not found", name);
        }
        return optional.get();
    }

    @Override
    public FlowGroup getById(String id) {
        Optional<FlowGroup> optional = flowGroupDao.findById(id);
        if (optional.isEmpty()) {
            throw new NotFoundException("Group {0} not found", id);
        }
        return optional.get();
    }

    @Override
    @Transactional
    public FlowGroup create(String name) {
        var email = sessionManager.getUserEmail();

        var group = new FlowGroup();
        group.setName(name);

        try {
            flowGroupDao.save(group);
            flowUsersDao.insert(group.getId(), Sets.newHashSet(email));
            return group;
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Group {0} already exists", name);
        }
    }

    @Override
    public void addToGroup(String flowName, String groupName) {
        if (flowName.equals(groupName)) {
            throw new ArgumentException("Cannot add to same group");
        }

        var groupId = FlowItem.ROOT_ID;
        if (!FlowItem.ROOT_NAME.equals(groupName)) {
            var group = get(groupName);
            groupId = group.getId();
        }

        var flow = getFlow(flowName);
        flow.setParentId(groupId);
        flowDao.save(flow);
    }

    @Override
    public void delete(String name) {
        flowGroupDao.deleteByName(name);
    }

    @Override
    public List<Flow> flows(String id) {
        return flowDao.findAllByParentId(id);
    }

    private Flow getFlow(String flowName) {
        var flow = flowDao.findByName(flowName);
        if (flow.isEmpty()) {
            throw new NotFoundException("the flow {0} not found", flowName);
        }
        return flow.get();
    }
}
