/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flow.platform.api.service.user;

import com.flow.platform.api.dao.user.ActionDao;
import com.flow.platform.api.dao.user.PermissionDao;
import com.flow.platform.api.dao.user.RoleDao;
import com.flow.platform.api.domain.user.Action;
import com.flow.platform.api.domain.user.Permission;
import com.flow.platform.api.domain.user.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lhl
 */

@Service
@Transactional
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private ActionDao actionDao;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private PermissionDao permissionDao;

    @Override
    public List<Action> list(Role role) {
        List<String> actionNames = permissionDao.list(role.getId());
        if (actionNames.isEmpty()) {
            return new ArrayList<>(0);
        }
        return actionDao.list(actionNames);
    }

    @Override
    public List<Role> list(Action action) {
        List<Integer> roleIds = permissionDao.list(action.getName());
        if (roleIds.isEmpty()) {
            return new ArrayList<>(0);
        }
        return roleDao.list(roleIds);
    }

    @Override
    public void assign(Role role, Set<Action> actions) {
        for (Action action : actions) {
            permissionDao.save(new Permission(role.getId(), action.getName()));
        }
    }

    @Override
    public void unAssign(Role role, Set<Action> actions) {
        for (Action action : actions) {
            permissionDao.delete(new Permission(role.getId(), action.getName()));
        }
    }
}
