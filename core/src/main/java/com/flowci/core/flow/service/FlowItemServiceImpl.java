package com.flowci.core.flow.service;

import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.flow.dao.FlowItemDao;
import com.flowci.core.flow.dao.FlowUsersDao;
import com.flowci.core.flow.domain.FlowItem;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class FlowItemServiceImpl implements FlowItemService {

    private final FlowItemDao flowItemDao;

    private final FlowUsersDao flowUsersDao;

    private final SessionManager sessionManager;

    @Override
    public List<FlowItem> list() {
        var email = sessionManager.getUserEmail();

        List<String> itemIdList = flowUsersDao.findAllFlowsByUserEmail(email);
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
