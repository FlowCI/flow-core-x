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

package com.flow.platform.cc.dao;

import org.hibernate.Session;

import java.io.Serializable;

/**
 * @author gy@fir.im
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
