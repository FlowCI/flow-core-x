package com.flowci.core.flow.service;

import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.flow.dao.FlowItemDao;
import com.flowci.core.flow.dao.FlowUserDao;
import com.flowci.core.flow.domain.FlowItem;
import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Service;

import java.util.*;

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

        List<String> itemIdList = flowUserDao.findAllFlowsByUserEmail(email);
        Set<FlowItem> items = flowItemDao.findAllByIdIn(itemIdList);

        // find all group id, since need to load group that user has permission for flow
        Set<String> groupIdSet = new HashSet<>(items.size() / 2);
        for (FlowItem item : items) {
            if (item.hasRootParent()) {
                continue;
            }
            groupIdSet.add(item.getParentId());
        }

        // load and remove duplicated groups, since some groups created by current user
        Set<FlowItem> groups = flowItemDao.findAllByIdIn(groupIdSet);
        items.removeAll(groups);

        List<FlowItem> list = new ArrayList<>(items.size() + groups.size());
        list.addAll(items);
        list.addAll(groups);
        return list;
    }

    @Override
    public boolean existed(String name) {
        return flowItemDao.existsByName(name);
    }
}
