package com.flow.platform.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;


/**
 * Created by Will on 17/6/12.
 */
public abstract class AbstractBaseDao<K extends Serializable, T> implements BaseDao<K, T> {

    @FunctionalInterface
    interface Executable<O> {
        O execute(Session session);
    }

    protected <O> O execute(Executable<O> ex) {
        Transaction transaction = null;
        Session session = getSession();

        try {
            transaction = session.beginTransaction();
            O result = ex.execute(session);
            transaction.commit();
            return result;
        } catch (Throwable e) {
            if (transaction != null && session.isOpen()) {
                transaction.rollback();
            }
            throw e;
        } finally {
            session.close();
        }
    }

    /**
     * Session Factory
     */
    @Autowired
    private SessionFactory sessionFactory;

    protected Session getSession() {
        return sessionFactory.openSession();
    }

    abstract Class<T> getEntityClass();

    /**
     * Get object by key
     *
     * @param key object id
     * @return T instance
     */
    public T get(final K key) {
        return execute(session -> session.get(getEntityClass(), key));
    }

    /**
     * Save single object
     *
     * @param obj
     * @return T with id if it is auto generated
     */
    public T save(final T obj) {
        return execute(session -> {
            session.save(obj);
            return obj;
        });
    }

    /**
     * Update object
     *
     * @param obj
     */
    public void update(final T obj) {
        execute(session -> {
            session.update(obj);
            return null;
        });
    }

    /**
     * Delete
     *
     * @param obj
     */
    public void delete(T obj) {
        execute(session -> {
           session.delete(obj);
           return null;
        });
    }
}
