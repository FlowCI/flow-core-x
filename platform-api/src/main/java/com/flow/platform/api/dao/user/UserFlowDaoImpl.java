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
import com.flow.platform.api.domain.user.UserFlow;
import com.flow.platform.api.domain.user.UserFlowKey;
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
public class UserFlowDaoImpl extends AbstractBaseDao<UserFlowKey, UserFlow> implements UserFlowDao {

    @Override
    protected Class<UserFlow> getEntityClass() {
        return UserFlow.class;
    }

    @Override
    protected String getKeyName() {
        return "key";
    }

    @Override
    public List<String> listByEmail(String email) {
        return execute(session -> session
            .createQuery("select key.flowPath from UserFlow where key.email = ?", String.class)
            .setParameter(0, email)
            .list());
    }

    @Override
    public Page<String> listByEmail(String email, Pageable pageable) {
        return execute(session -> {
            TypedQuery query = session
                .createQuery("select key.flowPath from UserFlow where key.email = ?", String.class)
                .setParameter(0, email);

            return PageUtil.buildPage(query, pageable);
        });
    }

    @Override
    public List<String> listByFlowPath(String flowPath) {
        return execute(session -> session
            .createQuery("select key.email from UserFlow where key.flowPath = ?", String.class)
            .setParameter(0, flowPath)
            .list());
    }

    @Override
    public Page<String> listByFlowPath(String flowPath, Pageable pageable) {
        return execute(session -> {
            TypedQuery query = session
                .createQuery("select key.email from UserFlow where key.flowPath = ?", String.class)
                .setParameter(0, flowPath);

            return PageUtil.buildPage(query, pageable);
        });
    }

    @Override
    public Long numOfUser(String flowPath) {
        return execute(session -> session
            .createQuery("select count(key.email) from UserFlow where key.flowPath = ?", Long.class)
            .setParameter(0, flowPath)
            .uniqueResult());
    }

    @Override
    public int deleteByEmail(String email) {
        return execute(session -> session
            .createQuery("delete from UserFlow where key.email = ?")
            .setParameter(0, email)
            .executeUpdate());
    }

    @Override
    public int deleteByFlowPath(String rootPath) {
        return execute(session -> session
            .createQuery("delete from UserFlow where key.flowPath = ?")
            .setParameter(0, rootPath)
            .executeUpdate());
    }
}
