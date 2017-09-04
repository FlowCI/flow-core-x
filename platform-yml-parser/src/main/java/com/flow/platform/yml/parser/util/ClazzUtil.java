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

package com.flow.platform.yml.parser.util;

import com.flow.platform.yml.parser.TypeAdaptorFactory;
import com.flow.platform.yml.parser.annotations.YmlSerializer;
import com.flow.platform.yml.parser.exception.YmlException;
import com.google.common.base.Strings;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import jdk.nashorn.internal.parser.TokenType;

/**
 * @author yh@firim
 */
public class ClazzUtil {

    public static <T> T read(Object o, Class<T> clazz) {

        try {

            T instance = clazz.newInstance();

            Class<?> raw = clazz;

            while (raw != Object.class) {
                Field[] fields = raw.getDeclaredFields();
                for (Field field : fields) {
                    // 获取 field 对应的值
                    YmlSerializer ymlSerializer = field.getAnnotation(YmlSerializer.class);

                    if (FieldUtil.noAnnotationField(field)) {
                        continue;
                    }

                    Object obj = ((Map) o).get(getAnnotationMappingName(field.getName(), ymlSerializer));

                    if (obj == null) {
                        continue;
                    }

                    // 必须的属性
                    if (FieldUtil.requiredField(field)) {
                        if (obj == null) {
                            throw new RuntimeException("required field");
                        }
                    }

                    field.setAccessible(true);
                    Type fieldType = MethodUtil.getClazz(field, clazz);
                    if (fieldType == null) {
                        fieldType = field.getGenericType();
                    }
                    try {
                        field.set(instance,
                            TypeAdaptorFactory.getAdaptor(fieldType).read(obj));
                    } catch (Throwable throwable) {
                        throw new YmlException("field set value error", throwable);
                    }
                }

                raw = raw.getSuperclass();
            }

            return instance;

        } catch (Throwable throwable) {
            throw new YmlException("ym error", throwable);
        }
    }


    public static <T> Object write(T clazz) {
        try {

            Map map = new LinkedHashMap();

            Class<?> raw = clazz.getClass();

            while (raw != Object.class) {
                Field[] fields = raw.getDeclaredFields();
                for (Field field : fields) {
                    // 获取 field 对应的值
                    YmlSerializer ymlSerializer = field.getAnnotation(YmlSerializer.class);

                    if (FieldUtil.noAnnotationField(field)) {
                        continue;
                    }

                    field.setAccessible(true);
                    Type fieldType = MethodUtil.getClazz(field, clazz.getClass());
                    if (fieldType == null) {
                        fieldType = field.getGenericType();
                    }
                    map.put(getAnnotationMappingName(field.getName(), ymlSerializer),
                        TypeAdaptorFactory.getAdaptor(fieldType).write(field.get(clazz)));
                }

                raw = raw.getSuperclass();
            }
            return map;
        } catch (Throwable throwable) {
            throw new YmlException("write yml error", throwable);
        }
    }

    private static String getAnnotationMappingName(String s, YmlSerializer ymlSerializer) {
        if (Strings.isNullOrEmpty(ymlSerializer.name())) {
            return s;
        } else {
            return ymlSerializer.name();
        }
    }

}
