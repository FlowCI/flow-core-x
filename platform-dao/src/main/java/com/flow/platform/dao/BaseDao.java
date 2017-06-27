package com.flow.platform.dao;

import org.hibernate.Session;

import java.io.Serializable;

/**
 * Created by gy@fir.im on 24/06/2017.
 * Copyright fir.im
 */
public interface BaseDao<K extends Serializable, T> {

    Session getSession();

    T get(final K key);

    T save(final T obj);

    void update(final T obj);

    void delete(final T obj);

    /**
     * Delete all data of table. should only used for test
     */
    int deleteAll();
}
