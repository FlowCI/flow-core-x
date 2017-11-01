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
package com.flow.platform.api.initializers;

import static com.flow.platform.api.config.AppConfig.DEFAULT_USER_EMAIL;
import static com.flow.platform.api.config.AppConfig.DEFAULT_USER_NAME;
import static com.flow.platform.api.config.AppConfig.DEFAULT_USER_PASSWORD;

import com.flow.platform.api.dao.user.ActionDao;
import com.flow.platform.api.dao.user.PermissionDao;
import com.flow.platform.api.dao.user.RoleDao;
import com.flow.platform.api.dao.user.UserDao;
import com.flow.platform.api.dao.user.UserRoleDao;
import com.flow.platform.api.domain.permission.Actions;
import com.flow.platform.api.domain.user.Action;
import com.flow.platform.api.domain.user.Permission;
import com.flow.platform.api.domain.user.PermissionKey;
import com.flow.platform.api.domain.user.Role;
import com.flow.platform.api.domain.user.SysRole;
import com.flow.platform.api.domain.user.User;
import com.flow.platform.api.domain.user.UserRole;
import com.flow.platform.api.domain.user.UserRoleKey;
import com.flow.platform.api.service.user.ActionService;
import com.flow.platform.api.service.user.PermissionService;
import com.flow.platform.api.service.user.RoleService;
import com.flow.platform.core.context.ContextEvent;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author lhl
 */

@Component
public class UserRoleInit implements ContextEvent {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserRoleDao userRoleDao;

    @Autowired
    private ActionService actionService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PermissionDao permissionDao;

    @Autowired
    private ActionDao actionDao;

    @Autowired
    private ThreadLocal<User> currentUser;

    @Override
    public void start() {

        // create sys user
        User user = userDao.get(DEFAULT_USER_EMAIL);
        if (user == null) {
            User sysUser = new User(DEFAULT_USER_EMAIL, DEFAULT_USER_NAME, DEFAULT_USER_PASSWORD);
            sysUser.setCreatedBy("system created");
            userDao.save(sysUser);
        }

        // create sys roleAdmin
        Role roleAdmin = roleDao.get(SysRole.ADMIN.name());
        if (roleAdmin == null) {
            Role sysRoleAdmin = new Role(SysRole.ADMIN.name(), "create default role for admin");
            sysRoleAdmin.setCreatedBy("system created");
            roleAdmin = roleDao.save(sysRoleAdmin);
        }

        // create sys roleUser
        Role roleUser = roleDao.get(SysRole.USER.name());
        if (roleUser == null) {
            Role sysRoleUser = new Role(SysRole.USER.name(), "create default role for user");
            sysRoleUser.setCreatedBy("system created");
            roleDao.save(sysRoleUser);
        }

        // create sys user_role relation
        Role role = roleService.find(SysRole.ADMIN.name());
        User sysUser = userDao.get(DEFAULT_USER_EMAIL);
        UserRole userRole = userRoleDao.get(new UserRoleKey(role.getId(), sysUser.getEmail()));
        if (userRole == null) {
            currentUser.set(sysUser);
            roleService.assign(sysUser, role);
        }

        // create sys action
        for(Actions item : Actions.values()) {
            currentUser.set(sysUser);
            if (actionDao.get(item.name()) == null){
                actionService.create(new Action(item.name()));
                permissionService.assign(roleAdmin, Sets.newHashSet(actionService.find(item.name())));
            }
        }

        Permission permission_flow_yml = permissionService.find(new PermissionKey(roleUser.getId(), Actions.FLOW_YML.name()));
        if (permission_flow_yml == null){
            permissionService.assign(roleUser, Sets.newHashSet(actionService.find(Actions.FLOW_YML.name())));
        }

        Permission permission_flow_show = permissionService.find(new PermissionKey(roleUser.getId(), Actions.FLOW_SHOW.name()));
        if (permission_flow_show == null){
            permissionService.assign(roleUser, Sets.newHashSet(actionService.find(Actions.FLOW_SHOW.name())));
        }

        Permission permission_job_show = permissionService.find(new PermissionKey(roleUser.getId(), Actions.JOB_SHOW.name()));
        if (permission_job_show == null){
            permissionService.assign(roleUser, Sets.newHashSet(actionService.find(Actions.JOB_SHOW.name())));
        }

        Permission permission_job_log = permissionService.find(new PermissionKey(roleUser.getId(), Actions.JOB_LOG.name()));
        if (permission_job_log == null){
            permissionService.assign(roleUser, Sets.newHashSet(actionService.find(Actions.JOB_LOG.name())));
        }

        Permission permission_job_create = permissionService.find(new PermissionKey(roleUser.getId(), Actions.JOB_CREATE.name()));
        if (permission_job_create == null){
            permissionService.assign(roleUser, Sets.newHashSet(actionService.find(Actions.JOB_CREATE.name())));
        }

        Permission permission_job_stop = permissionService.find(new PermissionKey(roleUser.getId(), Actions.JOB_STOP.name()));
        if (permission_job_stop == null){
            permissionService.assign(roleUser, Sets.newHashSet(actionService.find(Actions.JOB_STOP.name())));
        }

        Permission permission_job_yml = permissionService.find(new PermissionKey(roleUser.getId(), Actions.JOB_YML.name()));
        if (permission_job_yml == null){
            permissionService.assign(roleUser, Sets.newHashSet(actionService.find(Actions.JOB_YML.name())));
        }


        Permission permission_create_key = permissionService.find(new PermissionKey(roleUser.getId(), Actions.GENERATE_KEY.name()));
        if (permission_create_key == null){
            permissionService.assign(roleUser, Sets.newHashSet(actionService.find(Actions.GENERATE_KEY.name())));
        }


        Permission flow_create = permissionService.find(new PermissionKey(roleUser.getId(), Actions.FLOW_CREATE.name()));
        if (flow_create == null){
            permissionService.assign(roleUser, Sets.newHashSet(actionService.find(Actions.FLOW_CREATE.name())));
        }

        Permission user_show  = permissionService.find(new PermissionKey(roleUser.getId(), Actions.USER_SHOW.name()));
        if (user_show == null){
            permissionService.assign(roleUser, Sets.newHashSet(actionService.find(Actions.USER_SHOW.name())));
        }
    }

    @Override
    public void stop() {

    }
}
