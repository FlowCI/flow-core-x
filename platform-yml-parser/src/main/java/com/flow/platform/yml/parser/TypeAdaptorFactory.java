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

package com.flow.platform.yml.parser;

import com.flow.platform.yml.parser.adaptor.ArrayAdaptor;
import com.flow.platform.yml.parser.adaptor.CollectionAdaptor;
import com.flow.platform.yml.parser.adaptor.MapAdaptor;
import com.flow.platform.yml.parser.adaptor.PrimitiveAdaptor;
import com.flow.platform.yml.parser.adaptor.ReflectTypeAdaptor;
import com.flow.platform.yml.parser.adaptor.YmlAdaptor;
import com.flow.platform.yml.parser.exception.YmlParseException;
import com.flow.platform.yml.parser.util.TypeUtil;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author yh@firim
 */
public class TypeAdaptorFactory {

    private static Map<Type, YmlAdaptor> adaptorCache = new LinkedHashMap<>();

    private static Map<Type, YmlAdaptor> cacheFactories = new LinkedHashMap<>();

    private static final List<AdaptorSelector> selectorChain = new LinkedList<>();

    static {
        selectorChain.add(new MapAdaptorSelector());
        selectorChain.add(new PrimitiveAdaptorSelector());
        selectorChain.add(new ArrayAdaptorSelector());
        selectorChain.add(new CollectionAdaptorSelector());
        selectorChain.add(new ReflectTypeAdaptorSelector());
    }

    public static YmlAdaptor getAdaptor(Type type) {
        for (AdaptorSelector selector : selectorChain) {
            YmlAdaptor adaptor = selector.selectAdaptor(type);
            if (adaptor != null) {
                return adaptor;
            }
        }

        return null;
    }

    private interface AdaptorSelector {

        YmlAdaptor selectAdaptor(Type type);
    }

    private static class ArrayAdaptorSelector implements AdaptorSelector {

        @Override
        public YmlAdaptor selectAdaptor(Type type) {
            if (type instanceof GenericArrayType || type instanceof Class && ((Class<?>) type).isArray()) {
                Type componentType = TypeUtil.getArrayComponentType(type);
                YmlAdaptor<?> componentTypeAdapter = TypeAdaptorFactory.getAdaptor(componentType);
                return new ArrayAdaptor(TypeUtil.getRawType(componentType), componentTypeAdapter);
            }
            return null;
        }
    }


    private static class CollectionAdaptorSelector implements AdaptorSelector {

        @Override
        public YmlAdaptor selectAdaptor(Type type) {
            Class<?> rawType = TypeUtil.getRawType(type);
            if (!Collection.class.isAssignableFrom(rawType)) {
                return null;
            }

            Type elementType = TypeUtil.getCollectionElementType(type);
            YmlAdaptor<?> elementTypeAdapter = TypeAdaptorFactory.getAdaptor(elementType);
            return new CollectionAdaptor(rawType, elementTypeAdapter);
        }
    }

    private static class MapAdaptorSelector implements AdaptorSelector {

        @Override
        public YmlAdaptor selectAdaptor(Type type) {
            Class<?> rawType = TypeUtil.getRawType(type);

            // judge Map is rawType subclass
            if (!Map.class.isAssignableFrom(rawType)) {
                return null;
            }
            return new MapAdaptor();
        }
    }

    private static class PrimitiveAdaptorSelector implements AdaptorSelector {

        @Override
        public YmlAdaptor selectAdaptor(Type type) {
            Class<?> rawType = TypeUtil.getRawType(type);

            // judge rawType is primitive or not
            if (PrimitiveAdaptor.isUsePrimitive(rawType)) {
                return new PrimitiveAdaptor(rawType);
            }

            return null;
        }
    }

    private static class ReflectTypeAdaptorSelector implements AdaptorSelector {

        @Override
        public YmlAdaptor selectAdaptor(Type type) {
            Class<?> rawType = TypeUtil.getRawType(type);

            if (!Object.class.isAssignableFrom(rawType)) {
                return null;
            }
            return new ReflectTypeAdaptor(rawType);
        }
    }

}
