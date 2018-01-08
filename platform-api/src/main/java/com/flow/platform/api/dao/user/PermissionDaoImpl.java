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
package com.flow.platform.api.dao.user;

import com.flow.platform.api.dao.util.PageUtil;
import com.flow.platform.api.domain.user.Permission;
import com.flow.platform.api.domain.user.PermissionKey;
import com.flow.platform.core.dao.AbstractBaseDao;
import com.flow.platform.core.domain.Page;
import com.flow.platform.core.domain.Pageable;
import java.util.List;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

/**
 * @author lhl
 */
@Repository
public class PermissionDaoImpl extends AbstractBaseDao<PermissionKey, Permission> implements PermissionDao {

    @Override
    protected Class<Permission> getEntityClass() {
        return Permission.class;
    }

    @Override
    protected String getKeyName() {
        return "key";
    }

    @Override
    public List<Integer> list(String action) {
        return execute(session -> session
            .createQuery("select key.roleId from Permission where key.action = ?", Integer.class)
            .setParameter(0, action)
            .list());
    }

    @Override
    public Page<Integer> list(String action, Pageable pageable) {
        return execute(session -> {
            TypedQuery query = session
                .createQuery("select key.roleId from Permission where key.action = ?", Integer.class)
                .setParameter(0, action);

            return PageUtil.buildPage(query, pageable, new TotalSupplier() {
                @Override
                public long get() {
                    TypedQuery query = session
                        .createQuery("select count(*) from Permission where key.action = ?", Integer.class)
                        .setParameter(0, action);
                    return (long) query.getSingleResult();
                }
            });
        });
    }

    @Override
    public List<String> list(Integer roleId) {
        return execute(session -> session
            .createQuery("select key.action from Permission where key.roleId = ?", String.class)
            .setParameter(0, roleId)
            .list());
    }

    @Override
    public Page<String> list(Integer roleId, Pageable pageable) {
        return execute(session -> {
            TypedQuery query = session
                .createQuery("select key.action from Permission where key.roleId = ?", String.class)
                .setParameter(0, roleId);
            return PageUtil.buildPage(query, pageable, new TotalSupplier() {
                @Override
                public long get() {
                    TypedQuery query = session
                        .createQuery("select count(*) from Permission where key.roleId = ?", String.class)
                        .setParameter(0, roleId);
                    return (long) query.getSingleResult();
                }
            });
        });
    }

    @Override
    public Long numOfRole(String action) {
        return execute(session -> session
            .createQuery("select count(key.roleId) from Permission where key.action = ?", Long.class)
            .setParameter(0, action)
            .uniqueResult());
    }

    @Override
    public Long numOfAction(Integer roleId) {
        return execute(session -> session
            .createQuery("select count(key.action) from Permission where key.roleId = ?", Long.class)
            .setParameter(0, roleId)
            .uniqueResult());
    }
}
