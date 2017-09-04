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

import com.flow.platform.api.dao.user.PermissionDao;
import com.flow.platform.api.domain.user.Permission;
import com.flow.platform.core.exception.IllegalParameterException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lhl
 */
@Service(value = "permissionService")
@Transactional
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionDao permissionDao;

    @Override
    public Permission create(Permission permission) {
        if (findPermissionByAction(permission.getAction()) != null) {
            throw new IllegalParameterException("Action is already present");
        } else {
            permissionDao.save(permission);
            return permission;
        }
    }

    @Override
    public Permission update(Permission permission) {
        permissionDao.update(permission);
        return permission;
    }

    @Override
    public void delete(String action) {
        Permission permission = findPermissionByAction(action);
        permissionDao.delete(permission);
    }

    @Override
    public List<Permission> listPermissions() {
        return permissionDao.list();
    }

    private Permission findPermissionByAction(String action) {
        return permissionDao.get(action);
    }
}
