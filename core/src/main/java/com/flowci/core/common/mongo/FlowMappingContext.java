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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.util.TypeInformation;

/**
 * @author yang
 */
public class FlowMappingContext extends MongoMappingContext {

    private final Map<TypeInformation, String> collectionMapping = new HashMap<>(10);

    private ApplicationContext context;

    @Override
    protected <T> BasicMongoPersistentEntity<T> createPersistentEntity(TypeInformation<T> typeInformation) {
        String collection = collectionMapping.get(typeInformation);
        if (Objects.isNull(collection)) {
            return super.createPersistentEntity(typeInformation);
        }

        BasicMongoPersistentEntity<T> entity = new FlowPersistentEntity<>(typeInformation, collection);
        if (context != null) {
            entity.setApplicationContext(context);
        }

        return entity;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        super.setApplicationContext(applicationContext);
        this.context = applicationContext;
    }

    public void addCustomizedPersistentEntity(TypeInformation classTypeInformation, String collectionName) {
        collectionMapping.put(classTypeInformation, collectionName);
    }
}
