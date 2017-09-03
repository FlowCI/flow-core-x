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

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import org.apache.logging.log4j.util.ReflectionUtil;

/**
 * @author gy@fir.im
 */
public class ObjectUtil {

    /**
     * Convert field name xxYyZz to xx_yy_zz
     *
     * @return flatted field name
     */
    public static String convertFieldNameToFlat(String fieldName) {
        StringBuilder builder = new StringBuilder(fieldName.length() + 10);
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (Character.isUpperCase(c)) {
                builder.append("_").append(Character.toLowerCase(c));
                continue;
            }
            builder.append(c);
        }
        return builder.toString();
    }

    public static boolean assignValueToField(Field field, Object bean, Object value) {
        Class<?> aClass = bean.getClass();
        try {
            String setterMethodName = "set" + fieldNameForSetterGetter(field.getName());
            Method method = getDeclaredMethod(aClass, setterMethodName, field);
            method.invoke(bean, convertType(field, value));
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    public static Method getDeclaredMethod(Class clazz, String name, Field field) throws NoSuchFieldException {
        Method method;
        try {
            method = clazz.getDeclaredMethod(name, field.getType());
        } catch (NoSuchMethodException e) {
            clazz = clazz.getSuperclass();
            if(clazz == null){
                throw new NoSuchFieldException(name);
            }
            method = getDeclaredMethod(clazz, name, field);
        }
        return method;
    }



    private static Object convertType(Field field, Object value) {
        if (field.getType().equals(value.getClass())) {
            return value;
        }

        if (field.getType() == Integer.class) {
            return Integer.parseInt(value.toString());
        }

        if(field.getType().isAssignableFrom(value.getClass())){
            return value;
        }

        throw new IllegalArgumentException(
            String.format("Type from '%s' to '%s' not supported yet", value.getClass(), field.getType()));
    }

    public static Field[] getFields(Class<?> clazz) {
        return clazz.getDeclaredFields();
    }

    /**
     * Find not null fields
     *
     * @param clazz Target class
     * @param bean Target java bean instance
     * @param skipTypes Type list which not deal
     * @param checkNotEmpty Enable to check not empty fields for collection and map
     * @return field metadata and value of not null field
     */
    public static Map<Field, Object> findNotNullFieldValue(
        Class<?> clazz,
        Object bean,
        Set<Class<?>> skipTypes,
        Set<String> skipFields,
        boolean checkNotEmpty) {
        // find not null fields
        Field[] fields = getFields(clazz);
        Map<Field, Object> notNullFields = new HashMap<>(fields.length);

        if (skipTypes == null) {
            skipTypes = new HashSet<>(0);
        }

        if (skipFields == null) {
            skipFields = new HashSet<>(0);
        }

        for (Field field : fields) {
            if (skipFields.contains(field.getName()) || skipTypes.contains(field.getType())) {
                continue;
            }

            try {
                String fieldName = field.getName();
                fieldName = fieldNameForSetterGetter(fieldName);
                Method method = clazz.getDeclaredMethod("get" + fieldName);

                Object value = method.invoke(bean);
                if (value == null) {
                    continue;
                }

                if (checkNotEmpty && value instanceof Collection) {
                    Collection tmp = (Collection) value;
                    if (tmp.size() > 0) {
                        notNullFields.put(field, value);
                    }
                    continue;
                }

                if (checkNotEmpty && value instanceof Map) {
                    Map tmp = (Map) value;
                    if (tmp.size() > 0) {
                        notNullFields.put(field, value);
                    }
                    continue;
                }

                notNullFields.put(field, value);

            } catch (ReflectiveOperationException e) {
                continue;
            }
        }

        return notNullFields;
    }

    /**
     * Deep copy object by byte array stream
     */
    public static <T> T deepCopy(T source) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(source);
                oos.flush();
            }

            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
                return (T) ois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public static String fieldNameForSetterGetter(String fieldName) {
        return Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }
}
