package com.flow.platform.dao;

import com.flow.platform.domain.Agent;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.io.Serializable;


/**
 * Created by Will on 17/6/12.
 */
public class DaoBase {


    /**
     * Session Factory
     */
    private SessionFactory sessionFactory = SessionFactoryBuilder.getInstance().getSessionFactory();

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Session getSession(){
        return sessionFactory.openSession();
    }

    /**
     * save
     * @param var2
     * @param <T>
     * @return
     */
    public <T> T save(T var2){
        Session session = getSession();
        try {
            Transaction tx = session.beginTransaction();
            session.save(var2);
            tx.commit();
        }catch (RuntimeException e){
            session.getTransaction().rollback();
            throw  e;
        }finally {
            session.close();
        }
        return var2;
    }

    /**
     * update
     * @param var2
     * @param <T>
     * @return
     */
    public <T> T update(T var2){
        Session session = getSession();
        try {
            Transaction tx = session.beginTransaction();
//            session.update(var2);
            session.saveOrUpdate(var2);
            tx.commit();
        }catch (RuntimeException e){
            session.getTransaction().rollback();
            throw e;
        }finally {
            session.close();
        }
        return var2;
    }

    /**
     * Get
     * @param arg
     * @param id
     * @param <T>
     * @return
     */
    public <T> T get(Class<T> arg, Serializable id){
        Session session = getSession();
        T result;
        try {
            Transaction tx = session.beginTransaction();
            result = session.get(arg, id);
            tx.commit();
        }catch (RuntimeException e){
            session.getTransaction().rollback();
            throw e;
        }finally {
            session.close();
        }
        return result;
    }

    /**
     * Delete
     * @param object
     */
    public void delete(Object object){
        Session session = getSession();
        try {
            Transaction tx = session.beginTransaction();
            session.delete(object);
            tx.commit();
        }
        catch (RuntimeException e) {
            session.getTransaction().rollback();
            throw e; // or display error message
        }
        finally {
            session.close();
        }
    }



}
