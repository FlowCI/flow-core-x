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

package com.flow.platform.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author yang
 */
public class CollectionUtil {

    public static boolean isNullOrEmpty(final Collection collection) {
        return collection == null || collection.size() == 0;
    }

    public static boolean isNullOrEmpty(final Map map) {
        return map == null || map.size() == 0;
    }

    @SafeVarargs
    public static <T> boolean isNullOrEmpty(final T... objects) {
        return objects == null || objects.length == 0;
    }

    /**
     * Fetch property list from a standard java bean collection
     * - The type R must be public
     * - The property must has getXXX method
     *
     * @param <R> Root class type
     * @param <T> Property class type
     */
    public static <R, T> List<T> toPropertyList(final String property, final R... collection) {
        List<T> propertyCollection = new LinkedList<>();
        Method getterMethod = null;

        try {
            for (R item : collection) {
                if (getterMethod == null) {
                    getterMethod = getterMethod(item, property);
                }

                T propertyValue = (T) getterMethod.invoke(item);
                if (propertyValue != null) {
                    propertyCollection.add(propertyValue);
                }
            }
            return propertyCollection;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public static <R, T> List<T> toPropertyList(final String property, final Collection<R> collection) {
        return toPropertyList(property, collection.toArray());
    }

    public static <R, T> Map<T, R> toPropertyMap(final String property, final Collection<R> collection) {
        Map<T, R> map = new HashMap<>(collection.size());
        Method getterMethod = null;

        try {
            for (R item : collection) {
                if (getterMethod == null) {
                    getterMethod = getterMethod(item, property);
                }

                T propertyValue = (T) getterMethod.invoke(item);
                if (propertyValue != null) {
                    map.put(propertyValue, item);
                }
            }
            return map;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static <R, T> Method getterMethod(R instance, String property) throws ReflectiveOperationException {
        Class<?> itemClass = instance.getClass();
        Field field = itemClass.getDeclaredField(property);
        String getterMethodName = "get" + ObjectUtil.fieldNameForSetterGetter(field.getName());
        return itemClass.getDeclaredMethod(getterMethodName);
    }
}
