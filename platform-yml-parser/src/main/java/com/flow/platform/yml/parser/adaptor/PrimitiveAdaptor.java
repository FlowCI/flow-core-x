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

import com.flow.platform.yml.parser.factory.BaseFactory;
import com.flow.platform.yml.parser.util.TypeToken;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yh@firim
 */
public class PrimitiveAdaptor<E> extends BaseAdaptor<Object> {

    private static final Set<Class> WRAPPER_TYPES = new HashSet(Arrays.asList(
        Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
        Void.class));

    public static boolean isWrapperType(Class clazz) {
        return WRAPPER_TYPES.contains(clazz);
    }


    public final static BaseFactory FACTORY = new BaseFactory() {

        @Override
        public <T> BaseAdaptor<T> create(TypeToken<T> token) {
            Class<?> type = token.getRawType();

            // 判断是基本数据类型
            if (type.isPrimitive()) {
                return new PrimitiveAdaptor(type);
            }

            if(isWrapperType(type)){
                return new PrimitiveAdaptor(type);
            }

            if(type == String.class){
                return new PrimitiveAdaptor(type);
            }

            return null;
        }
    };

    private Class<E> componentType;

    public PrimitiveAdaptor(Class<E> componentType) {
        this.componentType = componentType;
    }

    @Override
    public Object read(Object o) {
        if (componentType.isAssignableFrom(o.getClass())) {
            return o;
        } else {
            throw new RuntimeException(String
                .format("data format not match componentType - %s, o.getClass - %s", componentType, o.getClass()));
        }
    }

    @Override
    public Object write(Object o) {
        return o;
    }
}
