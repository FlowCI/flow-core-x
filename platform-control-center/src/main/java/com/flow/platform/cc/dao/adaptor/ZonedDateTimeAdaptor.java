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

package com.flow.platform.cc.dao.adaptor;

import com.flow.platform.util.DateUtil;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * @author gy@fir.im
 */
public class ZonedDateTimeAdaptor extends BaseAdaptor {

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.BIGINT};
    }

    @Override
    public Class returnedClass() {
        return ZonedDateTime.class;
    }

    protected Type getTargetType() {
        return null;
    }

    /**
     * @param rs
     * @param names
     * @param session
     * @param owner
     * @return
     * @throws HibernateException
     * @throws SQLException
     */
    @Override
    public Object nullSafeGet(
        ResultSet rs,
        String[] names,
        SharedSessionContractImplementor session,
        Object owner) throws HibernateException, SQLException {

        long time = rs.getLong(names[0]);

        if (time == 0) {
            return null;
        }

        return DateUtil.fromDateForUTC(new Date(time));
    }

    /**
     * @param st
     * @param value
     * @param index
     * @param session
     * @throws HibernateException
     * @throws SQLException
     */
    @Override
    public void nullSafeSet(PreparedStatement st,
        Object value,
        int index,
        SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.BIGINT);
            return;
        }

        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
        long time = DateUtil.toDate(zonedDateTime).getTime();
        st.setLong(index, time);
    }
}