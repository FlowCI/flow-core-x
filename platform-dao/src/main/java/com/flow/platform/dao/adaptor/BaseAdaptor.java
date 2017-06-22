package com.flow.platform.dao.adaptor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Will on 17/6/20.
 */
public class BaseAdaptor implements UserType {

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.LONGVARCHAR};
    }

    @Override
    public Class returnedClass() {
        return String.class;
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
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {

//        if(rs.wasNull() == false){
            String str = rs.getString(names[0]);
            if (str == null) {
                return null;
            }
            return jsonToObject(str);
//        }
//        return null;
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
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if(value == null){
            st.setString(index, null);
        }else{
            String str = objectToJson(value);
            st.setString(index, str);
        }
    }

    Object jsonToObject(String json){
        Gson gson = new Gson();
        Object object = gson.fromJson(json, new TypeToken<Object>(){}.getType());
        return object;
    }

    String objectToJson(Object object){
        Gson gson = new Gson();
        String json = gson.toJson(object);
        return json;
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        if(value == null){
            return null;
        }
        String cp = objectToJson(value);
        return jsonToObject(cp);
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
}
