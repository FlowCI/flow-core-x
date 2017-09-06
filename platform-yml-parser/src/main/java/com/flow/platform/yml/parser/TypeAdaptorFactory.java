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
import com.flow.platform.yml.parser.adaptor.YmlAdaptor;
import com.flow.platform.yml.parser.adaptor.CollectionAdaptor;
import com.flow.platform.yml.parser.adaptor.MapAdaptor;
import com.flow.platform.yml.parser.adaptor.PrimitiveAdaptor;
import com.flow.platform.yml.parser.adaptor.ReflectTypeAdaptor;
import com.flow.platform.yml.parser.exception.YmlParseException;
import com.flow.platform.yml.parser.factory.YmlFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author yh@firim
 */
public class TypeAdaptorFactory {

    private static List<YmlFactory> factories = new LinkedList<>();

    private final static int ADAPTOR_CACHE_EXPIRE_SECOND = 3600 * 24;

    private final static int ADAPTOR_CACHE_CACHE_NUM = 100;

    private static Cache<Type, YmlAdaptor> adaptorCache = CacheBuilder
        .newBuilder()
        .expireAfterAccess(ADAPTOR_CACHE_EXPIRE_SECOND, TimeUnit.SECONDS)
        .maximumSize(ADAPTOR_CACHE_CACHE_NUM)
        .build();

    static {
        factories.add(MapAdaptor.FACTORY);
        factories.add(PrimitiveAdaptor.FACTORY);
        factories.add(CollectionAdaptor.FACTORY);
        factories.add(ArrayAdaptor.FACTORY);
        factories.add(ReflectTypeAdaptor.FACTORY);
    }

    private static Map<Type, YmlAdaptor> cacheFactories = new LinkedHashMap<>();

    public static YmlAdaptor getAdaptor(Type type) {
        YmlAdaptor ymlAdaptor = null;
        try {
            ymlAdaptor = adaptorCache.get(type, () -> {
                for (YmlFactory factory : factories) {
                    YmlAdaptor adaptor = factory.create(type);
                    if (adaptor != null) {
                        cacheFactories.put(type, adaptor);
                        return adaptor;
                    }
                }

                return null;
            });
        } catch (Throwable throwable) {
        }

        if (ymlAdaptor != null) {
            return ymlAdaptor;
        }

        throw new YmlParseException("Not found adaptor");
    }

}
