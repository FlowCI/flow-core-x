package com.flowci.core.flow.service;

import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.flow.dao.FlowItemDao;
import com.flowci.core.flow.dao.FlowUserDao;
import com.flowci.core.flow.domain.FlowItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlowItemServiceImpl implements FlowItemService {

    private final FlowItemDao flowItemDao;

    private final FlowUserDao flowUserDao;

    private final SessionManager sessionManager;

    public FlowItemServiceImpl(FlowItemDao flowItemDao, FlowUserDao flowUserDao, SessionManager sessionManager) {
        this.flowItemDao = flowItemDao;
        this.flowUserDao = flowUserDao;
        this.sessionManager = sessionManager;
    }

    @Override
    public List<FlowItem> list() {
        var email = sessionManager.getUserEmail();
        List<String> flows = flowUserDao.findAllFlowsByUserEmail(email);
        return flowItemDao.findAllByIdInOrderByCreatedAt(flows);
    }

    @Override
    public boolean existed(String name) {
        return flowItemDao.existsByName(name);
    }
}
