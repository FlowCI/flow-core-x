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

import com.flow.platform.yml.parser.exception.YmlException;
import com.flow.platform.yml.parser.factory.BaseFactory;
import com.flow.platform.yml.parser.util.PrimitiveUtil;
import com.flow.platform.yml.parser.util.TypeUtil;

/**
 * @author yh@firim
 */
public class PrimitiveAdaptor<E> extends BaseAdaptor<Object> {

    public final static BaseFactory FACTORY = type -> {

        Class<?> rawType = TypeUtil.getRawType(type);

        // judge rawType is primitive or not
        if (PrimitiveUtil.isUsePrimitive(rawType)) {
            return new PrimitiveAdaptor(rawType);
        }

        return null;
    };

    private Class<E> componentType;

    public PrimitiveAdaptor(Class<E> componentType) {
        this.componentType = componentType;
    }

    @Override
    public Object read(Object o) {

        if (componentType == Integer.class) {
            return Integer.parseInt(o.toString());
        }

        if (componentType == String.class) {
            return o.toString();
        }

        if (componentType.isAssignableFrom(o.getClass())) {
            return o;
        }

        throw new YmlException(String
            .format("data format not match componentType - %s, o.getClass - %s", componentType, o.getClass()));
    }

    @Override
    public Object write(Object o) {
        return o;
    }
}
