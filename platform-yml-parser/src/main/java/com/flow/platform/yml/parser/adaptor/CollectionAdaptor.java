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

package com.flow.platform.yml.parser.adaptor;

import com.flow.platform.yml.parser.TypeAdaptorFactory;
import com.flow.platform.yml.parser.exception.YmlParseException;
import com.flow.platform.yml.parser.factory.YmlFactory;
import com.flow.platform.yml.parser.util.TypeUtil;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author yh@firim
 */
public class CollectionAdaptor<E> extends YmlAdaptor<Object> {

    public final static YmlFactory FACTORY = type -> {

        Class<?> rawType = TypeUtil.getRawType(type);
        if(!Collection.class.isAssignableFrom(rawType)){
            return null;
        }

        Type elementType = TypeUtil.getCollectionElementType(type);
        YmlAdaptor<?> elementTypeAdapter = TypeAdaptorFactory.getAdaptor(elementType);
        return new CollectionAdaptor(rawType, elementTypeAdapter);
    };

    private Class<E> componentClazz;

    private YmlAdaptor<E> typeAdaptor;

    public CollectionAdaptor(Class<E> componentClazz, YmlAdaptor<E> typeAdaptor) {
        this.componentClazz = componentClazz;
        this.typeAdaptor = typeAdaptor;
    }

    @Override
    public Object read(Object o) {
        if (!Collection.class.isAssignableFrom(o.getClass())) {
            throw new YmlParseException("ArrayAdaptor Object Class not extends Collection");
        }

        List<E> list = new ArrayList<>();

        ((Collection) o).forEach(action -> {
            list.add(typeAdaptor.read(action));
        });

        return list;
    }

    @Override
    public Object write(Object object) {
        List<Object> objects = new ArrayList<>();
        for (E e : (Collection<E>) object) {
            objects.add(typeAdaptor.write(e));
        }

        return objects;
    }
}
