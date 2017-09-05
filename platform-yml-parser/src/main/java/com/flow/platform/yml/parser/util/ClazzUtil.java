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
import com.flow.platform.yml.parser.adaptor.BaseAdaptor;
import com.flow.platform.yml.parser.annotations.YmlSerializer;
import com.flow.platform.yml.parser.exception.YmlException;
import com.flow.platform.yml.parser.exception.YmlValidatorException;
import com.flow.platform.yml.parser.validator.BaseValidator;
import com.google.common.base.Strings;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import jdk.nashorn.internal.parser.TokenType;
import sun.invoke.empty.Empty;

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

                    // 必须的属性
                    if (FieldUtil.requiredField(field)) {
                        if (obj == null) {
                            throw new YmlException("required field");
                        }
                    }

                    if (obj == null) {
                        continue;
                    }

                    field.setAccessible(true);

                    try {
                        field.set(instance,
                            read(field, obj, clazz));

                        // validator field
                        validator(field, instance);
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

    public static <T> void validator(Field field, T instance) {
        YmlSerializer ymlSerializer = field.getAnnotation(YmlSerializer.class);
        if (ymlSerializer.validator() != Empty.class) {
            try {
                BaseValidator validator = (BaseValidator) ymlSerializer.validator().newInstance();
                if (validator.ReadValidator(field.get(instance)) == false) {
                    throw new YmlValidatorException(String.format("field - %s , validator - error", field.getName()));
                }
            } catch (Throwable throwable) {
                throw new YmlValidatorException(String.format("instance validator - error", throwable));
            }
        }
    }

    public static Object read(Field field, Object obj, Class<?> clazz) {
        YmlSerializer ymlSerializer = field.getAnnotation(YmlSerializer.class);

        Type fieldType = MethodUtil.getClazz(field, clazz);
        if (fieldType == null) {
            fieldType = field.getGenericType();
        }

        if (ymlSerializer.adaptor() == Empty.class) {
            return TypeAdaptorFactory.getAdaptor(fieldType).read(obj);
        }

        //采用 adaptor
        if (ymlSerializer.adaptor() != Empty.class) {
            try {
                BaseAdaptor instance = (BaseAdaptor) ymlSerializer.adaptor().newInstance();
                return instance.read(obj);
            } catch (Throwable throwable) {
                throw new YmlException("create instance adaptor", throwable);
            }
        }

        return null;
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

                    map.put(getAnnotationMappingName(field.getName(), ymlSerializer),
                        write(field, clazz));
                }

                raw = raw.getSuperclass();
            }
            return map;
        } catch (Throwable throwable) {
            throw new YmlException("write yml error", throwable);
        }
    }

    public static <T> Object write(Field field, T t) {
        YmlSerializer ymlSerializer = field.getAnnotation(YmlSerializer.class);

        Type fieldType = MethodUtil.getClazz(field, t.getClass());
        if (fieldType == null) {
            fieldType = field.getGenericType();
        }

        if (ymlSerializer.adaptor() == Empty.class) {
            try {
                return TypeAdaptorFactory.getAdaptor(fieldType).write(field.get(t));
            } catch (Throwable throwable) {
                throw new YmlException("field get error", throwable);
            }
        }

        //采用 adaptor
        if (ymlSerializer.adaptor() != Empty.class) {
            try {
                BaseAdaptor instance = (BaseAdaptor) ymlSerializer.adaptor().newInstance();
                return instance.write(field.get(t));
            } catch (Throwable throwable) {
                throw new YmlException("create instance adaptor", throwable);
            }
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
