package com.flow.platform.dao;

import org.hibernate.SessionFactory;

/**
 * Created by Will on 17/6/13.
 */
public class SessionFactoryBuilder {
    private static SessionFactoryBuilder instance;
    private SessionFactoryBuilder (){}
    public static SessionFactoryBuilder getInstance() {
        if (instance == null) {
            instance = new SessionFactoryBuilder();
        }
        return instance;
    }


    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private SessionFactory sessionFactory;
}
