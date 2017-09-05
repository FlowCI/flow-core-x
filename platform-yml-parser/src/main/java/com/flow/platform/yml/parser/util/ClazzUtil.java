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
import com.flow.platform.yml.parser.adaptor.YmlAdaptor;
import com.flow.platform.yml.parser.annotations.YmlSerializer;
import com.flow.platform.yml.parser.exception.YmlParseException;
import com.flow.platform.yml.parser.exception.YmlFormatException;
import com.flow.platform.yml.parser.validator.YmlValidator;
import com.google.common.base.Strings;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import sun.invoke.empty.Empty;

/**
 * @author yh@firim
 */
public class ClazzUtil {

    /**
     * read object value to clazz
     *
     * @param o object
     * @param clazz clazz
     * @param <T> clazz
     * @return clazz instance
     */
    public static <T> T read(Object o, Class<T> clazz) {

        T instance;
        try {
            instance = clazz.newInstance();
        } catch (Throwable throwable) {
            throw new YmlParseException(String.format("clazz - %s instance error", clazz.getName()), throwable);
        }

        Class<?> raw = clazz;

        while (raw != Object.class) {
            Field[] fields = raw.getDeclaredFields();
            for (Field field : fields) {

                // find field annotations
                YmlSerializer ymlSerializer = field.getAnnotation(YmlSerializer.class);

                //filter no annotations field
                if (FieldUtil.noAnnotationField(field)) {
                    continue;
                }

                //filter ignore field
                if (FieldUtil.ignoreField(field)) {
                    continue;
                }

                Object obj = ((Map) o).get(getAnnotationMappingName(field.getName(), ymlSerializer));

                // required field
                if (FieldUtil.requiredField(field)) {
                    if (obj == null) {
                        throw new YmlParseException(String.format("required field - %s", field.getName()));
                    }
                }

                if (obj == null) {
                    continue;
                }

                field.setAccessible(true);

                // read field value
                Object value = read(field, obj, clazz);

                try {
                    field.set(instance, value);

                } catch (Throwable throwable) {
                    throw new YmlParseException(String.format("field - %s set value error", field.getName()), throwable);
                }

                // validator field
                validator(field, instance);
            }

            raw = raw.getSuperclass();
        }

        return instance;
    }

    /**
     * validate field's value
     */
    public static <T> void validator(Field field, T instance) {
        YmlSerializer ymlSerializer = field.getAnnotation(YmlSerializer.class);
        if (ymlSerializer.validator() != Empty.class) {
            YmlValidator validator;
            Object value = null;
            try {
                validator = (YmlValidator) ymlSerializer.validator().newInstance();
                value = field.get(instance);
            } catch (Throwable throwable) {
                throw new YmlFormatException(String
                    .format("validator - %s instance  error - %s", ymlSerializer.validator().getName(), throwable));
            }

            if (validator.validate(value) == false) {
                throw new YmlFormatException(String.format("field - %s , validator - error", field.getName()));
            }

        }
    }

    /**
     * read field value from clazz, auto select adaptor or annotation support
     *
     * @param field field
     * @param obj yml
     * @param clazz field's clazz
     * @return Object
     */
    public static Object read(Field field, Object obj, Class<?> clazz) {
        YmlSerializer ymlSerializer = field.getAnnotation(YmlSerializer.class);

        // get field type
        Type fieldType = MethodUtil.getClazz(field, clazz);
        if (fieldType == null) {
            fieldType = field.getGenericType();
        }

        // auto select adaptor
        if (ymlSerializer.adaptor() == Empty.class) {
            return TypeAdaptorFactory.getAdaptor(fieldType).read(obj);
        }

        // annotation provide adaptor
        if (ymlSerializer.adaptor() != Empty.class) {
            try {
                YmlAdaptor instance = (YmlAdaptor) ymlSerializer.adaptor().newInstance();
                return instance.read(obj);
            } catch (Throwable throwable) {
                throw new YmlParseException("create instance adaptor", throwable);
            }
        }

        return null;
    }

    /**
     * model write field to object
     */
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
            throw new YmlParseException("write yml error", throwable);
        }
    }

    /**
     * auto select adaptor or annotation provider
     */
    public static <T> Object write(Field field, T t) {
        YmlSerializer ymlSerializer = field.getAnnotation(YmlSerializer.class);

        // get field type
        Type fieldType = MethodUtil.getClazz(field, t.getClass());
        if (fieldType == null) {
            fieldType = field.getGenericType();
        }

        // auto select adaptor
        if (ymlSerializer.adaptor() == Empty.class) {
            try {
                return TypeAdaptorFactory.getAdaptor(fieldType).write(field.get(t));
            } catch (Throwable throwable) {
                throw new YmlParseException(String.format("field - %s get error", field.getName()), throwable);
            }
        }

        //annotation provider adaptor
        if (ymlSerializer.adaptor() != Empty.class) {
            try {
                YmlAdaptor instance = (YmlAdaptor) ymlSerializer.adaptor().newInstance();
                return instance.write(field.get(t));
            } catch (Throwable throwable) {
                throw new YmlParseException(
                    String.format("create instance adaptor - %s error", ymlSerializer.adaptor().getName()), throwable);
            }
        }

        return null;
    }

    /**
     * get field mapping name
     */
    private static String getAnnotationMappingName(String s, YmlSerializer ymlSerializer) {
        if (Strings.isNullOrEmpty(ymlSerializer.name())) {
            return s;
        } else {
            return ymlSerializer.name();
        }
    }

}
