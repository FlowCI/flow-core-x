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
package com.flow.platform.api.dao;

import static com.flow.platform.core.dao.PageUtil.buildPage;

import com.flow.platform.api.domain.credential.Credential;
import com.flow.platform.api.domain.credential.CredentialType;
import com.flow.platform.core.dao.AbstractBaseDao;
import com.flow.platform.core.domain.Page;
import com.flow.platform.core.domain.Pageable;
import com.google.common.base.Strings;
import java.util.Collection;
import java.util.List;
import javax.persistence.TypedQuery;
import org.springframework.stereotype.Repository;


/**
 * @author lhl
 */

@Repository
public class CredentialDaoImpl extends AbstractBaseDao<String, Credential> implements CredentialDao {

    @Override
    protected Class<Credential> getEntityClass() {
        return Credential.class;
    }

    @Override
    protected String getKeyName() {
        return "name";
    }

    @Override
    public boolean exist(String name) {
        String result = execute(session -> session
            .createQuery("select name from Credential where name = :name", String.class)
            .setParameter("name", name)
            .uniqueResult());

        return !Strings.isNullOrEmpty(result);
    }

    @Override
    public List<Credential> listByType(Collection<CredentialType> types) {
        return execute(session -> session
            .createQuery("from Credential where type in :types", getEntityClass())
            .setParameterList("types", types)
            .list());
    }


    @Override
    public Page<Credential> listByType(Collection<CredentialType> types, Pageable pageable) {
        return execute(session -> {
            TypedQuery query = session
                .createQuery("from Credential where type in :types", getEntityClass())
                .setParameterList("types", types);

            return buildPage(query, pageable, new TotalSupplier() {
                @Override
                public long get() {
                    TypedQuery query = session
                        .createQuery("select count(*) from Credential where type in :types")
                        .setParameterList("types", types);
                    return (long) query.getSingleResult();
                }
            });
        });
    }
}

