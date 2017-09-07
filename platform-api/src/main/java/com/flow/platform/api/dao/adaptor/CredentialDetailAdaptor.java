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

package com.flow.platform.api.dao.adaptor;

import com.flow.platform.api.domain.credential.CredentialType;
import com.flow.platform.core.dao.adaptor.BaseAdaptor;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author lhl
 */
public class CredentialDetailAdaptor extends BaseAdaptor {

    @Override
    protected Type getTargetType() {
        return null;
    }

    @Override
    public Class returnedClass() {
        return null;
    }

    @Override
    public Object nullSafeGet(ResultSet rs,
                              String[] names,
                              SharedSessionContractImplementor session,
                              Object owner) throws HibernateException, SQLException {

        String detailRaw = rs.getString(names[0]);
        if (detailRaw == null) {
            return null;
        }

        // according to credential.hbm.xml property sequence
        String typeRaw = rs.getString(2);
        CredentialType type = CredentialType.valueOf(typeRaw);

        return GSON.fromJson(detailRaw, type.getClazz());
    }
}
