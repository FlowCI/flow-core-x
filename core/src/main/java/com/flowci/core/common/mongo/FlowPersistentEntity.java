/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.common.mongo;

import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentEntity;
import org.springframework.data.util.TypeInformation;

/**
 * @author yang
 */
public class FlowPersistentEntity<T> extends BasicMongoPersistentEntity<T> {

    private final String collection;

    public FlowPersistentEntity(TypeInformation<T> typeInformation, String collection) {
        super(typeInformation);
        this.collection = collection;
    }

    @Override
    public String getCollection() {
        return collection;
    }
}
