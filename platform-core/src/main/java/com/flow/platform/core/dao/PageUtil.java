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

package com.flow.platform.core.dao;

import com.flow.platform.core.dao.AbstractBaseDao.TotalSupplier;
import com.flow.platform.core.domain.Page;
import com.flow.platform.core.domain.Pageable;
import javax.persistence.TypedQuery;

/**
 * @author yh@firim
 */
public class PageUtil {

    public static <T> Page<T> buildPage(TypedQuery query, Pageable pageable, TotalSupplier totalSupplier) {
        query.setFirstResult(pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        return new Page<T>(query.getResultList(), pageable.getPageSize(), pageable.getPageNumber(), totalSupplier);
    }
}
