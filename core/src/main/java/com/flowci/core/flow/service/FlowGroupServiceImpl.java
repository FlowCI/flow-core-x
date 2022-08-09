package com.flowci.core.flow.service;

import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.FlowGroupDao;
import com.flowci.core.flow.dao.FlowUserDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.FlowGroup;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FlowGroupServiceImpl implements FlowGroupService {

    private final FlowDao flowDao;

    private final FlowGroupDao flowGroupDao;

    private final FlowUserDao flowUserDao;

    private final SessionManager sessionManager;

    @Autowired
    public FlowGroupServiceImpl(FlowDao flowDao,
                                FlowGroupDao flowGroupDao,
                                FlowUserDao flowUserDao,
                                SessionManager sessionManager) {
        this.flowDao = flowDao;
        this.flowGroupDao = flowGroupDao;
        this.flowUserDao = flowUserDao;
        this.sessionManager = sessionManager;
    }

    @Override
    public FlowGroup get(String name) {
        Optional<FlowGroup> optional = flowGroupDao.findByName(name);
        if (optional.isEmpty()) {
            throw new NotFoundException("Group {0} not found", name);
        }
        return optional.get();
    }

    @Override
    public FlowGroup create(String name) {
        var email = sessionManager.getUserEmail();

        var group = new FlowGroup();
        group.setName(name);

        try {
            flowGroupDao.save(group);
            flowUserDao.insert(group.getId(), Sets.newHashSet(email));
            return group;
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Group {0} already exists", name);
        }
    }

    @Override
    public void addToGroup(String flowName, String groupName) {
        var group = get(groupName);
        var flow = getFlow(flowName);
        flow.setParentId(group.getId());
        flowDao.save(flow);

        // add all users from current flow to parent group
        var users = flowUserDao.findAllUsers(flow.getId());
        flowUserDao.insert(group.getId(), Sets.newHashSet(users));
    }

    @Override
    public void removeFromGroup(String flowName) {
        var flow = getFlow(flowName);
        flow.setParentId(null);
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
