package com.flowci.core.flow.service;

import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.FlowGroupDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.FlowGroup;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class FlowGroupServiceImpl implements FlowGroupService {

    private final FlowDao flowDao;

    private final FlowGroupDao flowGroupDao;

    @Autowired
    public FlowGroupServiceImpl(FlowDao flowDao, FlowGroupDao flowGroupDao) {
        this.flowDao = flowDao;
        this.flowGroupDao = flowGroupDao;
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
    public List<FlowGroup> list(Collection<String> ids) {
        return flowGroupDao.findAllByIdIn(ids);
    }

    @Override
    public FlowGroup create(String name) {
        try {
            var group = new FlowGroup();
            group.setName(name);
            return flowGroupDao.save(group);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Group {0} already exists", name);
        }
    }

    @Override
    public void addToGroup(String flowName, String groupName) {
        var group = get(groupName);
        var flow = getFlow(flowName);
        flow.setGroupId(group.getId());
        flowDao.save(flow);
    }

    @Override
    public void removeFromGroup(String flowName) {
        var flow = getFlow(flowName);
        flow.setGroupId(null);
        flowDao.save(flow);
    }

    @Override
    public void delete(String name) {
        flowGroupDao.deleteByName(name);
    }

    @Override
    public List<Flow> flows(String id) {
        return flowDao.findAllByGroupId(id);
    }

    private Flow getFlow(String flowName) {
        var flow = flowDao.findByName(flowName);
        if (flow.isEmpty()) {
            throw new NotFoundException("the flow {0} not found", flowName);
        }
        return flow.get();
    }
}
