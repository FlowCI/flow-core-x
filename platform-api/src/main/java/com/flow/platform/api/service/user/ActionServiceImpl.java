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
import com.flow.platform.api.dao.user.RolesPermissionsDao;
import com.flow.platform.api.domain.user.Action;
import com.flow.platform.core.exception.IllegalParameterException;
import com.google.common.base.Strings;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lhl
 */
@Service
@Transactional
public class ActionServiceImpl implements ActionService {

    @Autowired
    private ActionDao actionDao;

    @Autowired
    private RolesPermissionsDao rolesPermissionsDao;

    @Override
    public Action find(String name) {
        Action action = actionDao.get(name);
        if (action == null) {
            throw new IllegalParameterException("Cannot find action by name: " + name);
        }
        return action;
    }

    @Override
    public Action create(Action action) {
        final String name = action.getName();

        if (Strings.isNullOrEmpty(action.getName())) {
            throw new IllegalParameterException("Action name must be provided");
        }

        if (actionDao.get(action.getName()) != null) {
            throw new IllegalParameterException("Action name '" + action.getName() + "' is already presented");
        }

        if (Strings.isNullOrEmpty(action.getAlias())) {
            action.setAlias(name);
        }

        return actionDao.save(action);
    }

    @Override
    public void update(Action action) {
        actionDao.update(action);
    }

    @Override
    public void delete(String name) {
        // TODO: check action has assigned to role
        Action action = find(name);
        actionDao.delete(action);
    }

    @Override
    public List<Action> list() {
        return actionDao.list();
    }
}
