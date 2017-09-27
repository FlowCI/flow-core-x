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
import com.flow.platform.api.domain.request.ActionParam;
import com.flow.platform.api.domain.user.Action;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.google.common.base.Strings;
import java.util.Collection;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
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
    private PermissionDao permissionDao;

    @Autowired
    protected ThreadLocal<User> currentUser;


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

        action.setCreatedBy(currentUser.get().getEmail());

        return actionDao.save(action);
    }

    @Override
    public Action update(String name, ActionParam body) {
        Action action = find(name);
        action.setAlias(body.getAlias());
        action.setDescription(body.getDescription());
        action.setCreatedBy(currentUser.get().getEmail());
        action.setTag(body.getTag());

        actionDao.update(action);
        return action;
    }

    @Override
    public void delete(String name) {
        Action action = find(name);

        Long numOfRole = permissionDao.numOfRole(name);
        if (numOfRole > 0L) {
            String err = String.format("Cannot delete action since '%s' roles assigned", numOfRole);
            throw new IllegalStatusException(err);
        }

        actionDao.delete(action);
    }

    @Override
    public List<Action> list() {
        return actionDao.list();
    }

    @Override
    public List<Action> list(Collection<String> actions) {
        return actionDao.list(actions);
    }

}
