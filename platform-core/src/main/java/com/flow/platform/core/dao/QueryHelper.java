package com.flow.platform.core.dao;

import com.flow.platform.util.StringUtil;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.TypedQuery;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

public class QueryHelper {


    private QueryHelper() {
    }

    public static Builder Builder() {
        return new Builder();
    }

    public static class Builder {

        private String select;

        private String from;

        private String where;

        private Map<String, Object> parameter = new HashMap<>();

        public Builder select(String select) {
            this.select = select;
            return this;
        }

        public Builder from(String from) {
            this.from = from;
            return this;
        }

        public Builder where(String where) {
            this.where = where;
            return this;
        }

        public Builder parameter(String key, Object value) {
            this.parameter.put(key, value);
            return this;
        }

        public NativeQuery createNativeQuery(Session session) {

            StringBuilder query = new StringBuilder();

            query.append("SELECT " + this.select);
            query.append(" FROM " + this.from);

            if (!StringUtil.isNullOrEmptyForItems(this.where)){
                query.append(" WHERE " + this.where);
            }

            NativeQuery nativeQuery = session.createNativeQuery(query.toString());
            for (Map.Entry<String, Object> entry : this.parameter.entrySet()) {
                nativeQuery.setParameter(entry.getKey(), entry.getValue());
            }
            return nativeQuery;
        }

        public TypedQuery createTypedQuery(Session session, Class clazz){

            StringBuilder query = new StringBuilder();

            query.append("SELECT " + this.select);
            query.append(" FROM " + this.from);

            if (!StringUtil.isNullOrEmptyForItems(this.where)){
                query.append(" WHERE " + this.where);
            }

            TypedQuery typedQuery = session.createQuery(query.toString(), clazz);
            for (Map.Entry<String, Object> entry : this.parameter.entrySet()) {
                typedQuery.setParameter(entry.getKey(), entry.getValue());
            }
            return typedQuery;

        }

    }

}
