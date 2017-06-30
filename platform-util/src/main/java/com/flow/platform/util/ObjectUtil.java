package com.flow.platform.util;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by gy@fir.im on 26/06/2017.
 * Copyright fir.im
 */
public class ObjectUtil {

    /**
     * Find not null fields
     *
     * @param clazz         Target class
     * @param bean          Target java bean instance
     * @param skipTypes     Type list which not deal
     * @param checkNotEmpty Enable to check not empty fields for collection and map
     * @return field metadata and value of not null field
     */
    public static Map<Field, Object> findNotNullFieldValue(Class<?> clazz,
                                                           Object bean,
                                                           Set<Class<?>> skipTypes,
                                                           Set<String> skipFields,
                                                           boolean checkNotEmpty) {
        // find not null fields
        Field[] fields = clazz.getDeclaredFields();
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
                fieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
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

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                continue;
            }
        }

        return notNullFields;
    }

    /**
     * Deep copy object by byte array stream
     *
     * @param source
     * @param <T>
     * @return
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
}
