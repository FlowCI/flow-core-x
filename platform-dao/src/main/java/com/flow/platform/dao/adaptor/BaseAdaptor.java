package com.flow.platform.dao.adaptor;

import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.ObjectUtil;
import com.google.gson.Gson;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Created by Will on 17/6/20.
 */
public abstract class BaseAdaptor implements UserType {

    public static final Gson GSON = Jsonable.GSON_CONFIG;

    protected abstract Type getTargetType();

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.LONGVARCHAR};
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return x == y || !(x == null || y == null) && x.equals(y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return null == x ? 0 : x.hashCode();
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
    public Object nullSafeGet(ResultSet rs,
                              String[] names,
                              SharedSessionContractImplementor session,
                              Object owner) throws HibernateException, SQLException {
        String str = rs.getString(names[0]);
        if (str == null) {
            return null;
        }
        return jsonToObject(str);
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

        // set to null
        if (value == null) {
            st.setString(index, null);
            return;
        }

        // value already in string type
        if (value instanceof String) {
            st.setString(index, (String) value);
            return;
        }

        String str = objectToJson(value);
        st.setString(index, str);
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return ObjectUtil.deepCopy(value);
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) deepCopy(value);
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return deepCopy(cached);
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return deepCopy(original);
    }

    protected Object jsonToObject(String json) {
        return GSON.fromJson(json, getTargetType());
    }

    protected String objectToJson(Object object) {
        return GSON.toJson(object);
    }
}
