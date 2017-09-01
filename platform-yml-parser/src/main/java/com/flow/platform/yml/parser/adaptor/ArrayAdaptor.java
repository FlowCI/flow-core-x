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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author gyfirim
 */
public class ArrayAdaptor<E> extends TypeAdaptor {

    private Class<E> componentType;

    public ArrayAdaptor(Class<E> eClass) {
        this.componentType = eClass;
    }

    @Override
    public <T> void write(Object o, Class<T> clazz) {

    }

    @Override
    public <T> Object read(Object o, Class<T> clazz) {
        List<E> list = new ArrayList<>();
        ((Collection)o).forEach(action -> {
            list.add((E) TypeAdaptorFactory.getTypeAdaptor(componentType).read(action, componentType));
        });

        Object array = Array.newInstance(componentType, list.size());
        for (int i = 0; i < list.size(); i++) {
            Array.set(array, i, list.get(i));
        }
        return array;
    }
}
