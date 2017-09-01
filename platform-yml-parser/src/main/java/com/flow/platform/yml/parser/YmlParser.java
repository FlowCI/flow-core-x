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

import com.flow.platform.yml.parser.annotations.YmlSerializer;
import com.google.common.base.Strings;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yh@firim
 */
public class YmlParser {

    public static <T> T fromObject(Object o, Class<T> t) {
        return (T) TypeAdaptorFactory.getTypeAdaptor(t).read(o, t);
    }

    public static <T> Object toObject(T t) {
        return null;
    }


    public static <T> T build(Object o, Class<T> clazz) {
        try {

            T instance = clazz.newInstance();

            //过滤带有注释的field
            filterAnnotationField(clazz).forEach((s, field) -> {
                // 获取 field 对应的值
                YmlSerializer ymlSerializer = field.getAnnotation(YmlSerializer.class);

                Object obj = ((Map) o).get(getAnnotationMappingName(s, ymlSerializer));

                // 必须的属性
                if (requiredField(field)) {
                    if (obj == null) {
                        throw new RuntimeException("required field");
                    }
                }

                if (obj != null) {
                    field.setAccessible(true);
                    try {
                        field.set(instance,
                            TypeAdaptorFactory.getTypeAdaptor(ymlSerializer).read(obj, ymlSerializer.value()));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            });

            //过滤带有注释的方法
            filterAnnotationMethods(clazz).forEach((s, method) -> {
                YmlSerializer ymlSerializer = method.getAnnotation(YmlSerializer.class);
                // 获取 field 对应的值
                Object obj = ((Map) o).get(getAnnotationMappingName(s, ymlSerializer));

                // 必须的属性
                if (requiredMethod(method)) {
                    if (obj == null) {
                        throw new RuntimeException("required field");
                    }
                }

                if (obj != null) {
                    method.setAccessible(true);
                    try {
                        method.invoke(instance,
                            TypeAdaptorFactory.getTypeAdaptor(ymlSerializer).read(obj, ymlSerializer.value()));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            });

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

    // 必填的字段
    private static Boolean requiredField(Field field) {
        YmlSerializer annotation = field.getAnnotation(YmlSerializer.class);
        if (annotation == null) {
            return false;
        }

        if (annotation.required() == true) {
            return true;
        }

        return false;
    }

    // 必填的字段
    private static Boolean requiredMethod(Method method) {
        YmlSerializer annotation = method.getAnnotation(YmlSerializer.class);
        if (annotation == null) {
            return false;
        }

        if (annotation.required() == true) {
            return true;
        }

        return false;
    }

    //过滤有标识的field
    private static Map<String, Field> filterAnnotationField(Class<?> clazz) {
        Map<String, Field> fieldMap = new HashMap<>();
        allFields(clazz).forEach((s, field) -> {
            YmlSerializer annotation = field.getAnnotation(YmlSerializer.class);
            if (annotation != null) {
                fieldMap.put(s, field);
            }
        });
        return fieldMap;
    }

    // 查找所有的field
    private static Map<String, Field> allFields(Class<?> clazz) {
        Map<String, Field> map = new HashMap<>();

        while (true) {
            if (clazz == null) {
                break;
            }

            for (Field field : clazz.getDeclaredFields()) {
                map.put(field.getName(), field);
            }

            clazz = clazz.getSuperclass();
        }

        return map;
    }


    // get annotation's method
    private static Map<String, Method> filterAnnotationMethods(Class<?> clazz) {
        Map<String, Method> maps = new HashMap<>();

        allMethods(clazz).forEach((s, method) -> {
            YmlSerializer ymlSerializer = method.getAnnotation(YmlSerializer.class);
            if (ymlSerializer != null) {
                maps.put(method.getName(), method);
            }
        });

        return maps;
    }

    //get all declared methods
    private static Map<String, Method> allMethods(Class<?> clazz) {
        Map<String, Method> map = new HashMap<>();

        while (true) {
            if (clazz == null) {
                break;
            }

            Method[] methods = clazz.getDeclaredMethods();

            for (Method method : methods) {
                if (map.get(method.getName()) == null) {
                    map.put(method.getName(), method);
                }
            }

            clazz = clazz.getSuperclass();
        }

        return map;
    }
}
