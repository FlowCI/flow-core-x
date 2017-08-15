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

import com.flow.platform.api.domain.Credential;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

/**
 * @author lhl
 */

@Repository(value = "credentialDao")
public class CredentialDaoImpl extends AbstractBaseDao<String, Credential> implements CredentialDao {
    @Override
    Class<Credential> getEntityClass() {
        return Credential.class;
    }

    @Override
    String getKeyName() {
        return "name";
    }

    @Override
    public List<Credential> list() {
        return execute((Session session) -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Credential> select = builder.createQuery(Credential.class);
            Root<Credential> credential = select.from(Credential.class);
            Predicate condition = builder.not(credential.get("name").isNull());
            select.where(condition);
            return session.createQuery(select).list();
        });
    }
}

