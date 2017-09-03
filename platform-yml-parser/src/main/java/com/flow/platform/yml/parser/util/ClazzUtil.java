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
import com.google.common.base.Strings;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Map;
import jdk.nashorn.internal.parser.TokenType;

/**
 * @author yh@firim
 */
public class ClazzUtil {

    public static <T> T build(Object o, Class<T> clazz) {
        try {

            T instance = clazz.newInstance();

//            //过滤带有注释的field
//            FieldUtil.filterAnnotationField(clazz).forEach((s, field) -> {
//                // 获取 field 对应的值
//                YmlSerializer ymlSerializer = field.getAnnotation(YmlSerializer.class);
//
//                Object obj = ((Map) o).get(getAnnotationMappingName(s, ymlSerializer));
//
//                // 必须的属性
//                if (FieldUtil.requiredField(field)) {
//                    if (obj == null) {
//                        throw new RuntimeException("required field");
//                    }
//                }
//
//                if (obj != null) {
//                    field.setAccessible(true);
//                    try {
//                        field.set(instance,
//                            TypeAdaptorFactory.getAdaptor(field.getType()).read(obj));
//                    } catch (IllegalAccessException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });

            TypeToken<?> type = TypeToken.get(clazz);
            Class<?> raw = clazz;

            while (raw != Object.class) {
                Field[] fields = raw.getDeclaredFields();
                for (Field field : fields) {
                    // 获取 field 对应的值
                    YmlSerializer ymlSerializer = field.getAnnotation(YmlSerializer.class);

                    if (ymlSerializer == null) {
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
                    Type fieldType = $Gson$Types.resolve(type.getType(), raw, field.getGenericType());
                    try {
                        field.set(instance,
                            TypeAdaptorFactory.getAdaptor(TypeToken.get(fieldType)).read(obj));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }

                type = TypeToken.get($Gson$Types.resolve(type.getType(), raw, raw.getGenericSuperclass()));
                raw = type.getRawType();
            }

//            //过滤带有注释的方法
//            MethodUtil.filterAnnotationMethods(clazz).forEach((s, method) -> {
//                YmlSerializer ymlSerializer = method.getAnnotation(YmlSerializer.class);
//                // 获取 field 对应的值
//                Object obj = ((Map) o).get(getAnnotationMappingName(s, ymlSerializer));
//
//                // 必须的属性
//                if (MethodUtil.requiredMethod(method)) {
//                    if (obj == null) {
//                        throw new RuntimeException("required field");
//                    }
//                }
//
//                if (obj != null) {
//                    method.setAccessible(true);
//                    try {
//                        method.invoke(instance,
//                            TypeAdaptorFactory.getAdaptor(ymlSerializer.value()).read(obj));
//                    } catch (IllegalAccessException e) {
//                        e.printStackTrace();
//                    } catch (InvocationTargetException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });

            return instance;

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getAnnotationMappingName(String s, YmlSerializer ymlSerializer) {
        if (Strings.isNullOrEmpty(ymlSerializer.name())) {
            return s;
        } else {
            return ymlSerializer.name();
        }
    }
}
