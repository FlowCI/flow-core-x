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
import com.flow.platform.yml.parser.factory.BaseFactory;
import com.flow.platform.yml.parser.util.$Gson$Types;
import com.flow.platform.yml.parser.util.TypeToken;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author yh@firim
 */
public class ArrayAdaptor<E> extends BaseAdaptor<Object> {

    public final static BaseFactory FACTORY = new BaseFactory() {
        @Override
        public <T> BaseAdaptor<T> create(TypeToken<T> typeToken) {
            Type type = typeToken.getType();
            if (!(type instanceof GenericArrayType || type instanceof Class && ((Class<?>) type).isArray())) {
                return null;
            }
            Type componentType = $Gson$Types.getArrayComponentType(type);
            BaseAdaptor<?> componentTypeAdapter = TypeAdaptorFactory.getAdaptor(TypeToken.get(componentType));

            return new ArrayAdaptor($Gson$Types.getRawType(componentType), componentTypeAdapter);
        }
    };

    private Class<E> componentType;

    private BaseAdaptor<E> baseAdaptor;

    public ArrayAdaptor(Class<E> componentType, BaseAdaptor<E> baseAdaptor) {
        this.componentType = componentType;
        this.baseAdaptor = baseAdaptor;
    }

    @Override
    public Object read(Object o) {
        List<E> list = new ArrayList<>();
        ((Collection)o).forEach(action -> {
            list.add(baseAdaptor.read(action));
        });

        Object array = Array.newInstance(componentType, list.size());
        for (int i = 0; i < list.size(); i++) {
            Array.set(array, i, list.get(i));
        }
        return array;
    }

    @Override
    public void write(Object o, Object object) {

    }
}
