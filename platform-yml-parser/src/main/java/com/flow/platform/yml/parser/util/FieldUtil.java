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

import com.flow.platform.yml.parser.annotations.YmlSerializer;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yh@firim
 */
public class FieldUtil {

    /**
     * detect required field from annotation
     *
     * @param field field
     * @return True or false
     */
    public static Boolean requiredField(Field field) {
        YmlSerializer annotation = field.getAnnotation(YmlSerializer.class);
        if (annotation == null) {
            return false;
        }

        if (annotation.required() == true) {
            return true;
        }

        return false;
    }

    /**
     * filter no annotation field
     */
    public static Boolean noAnnotationField(Field field) {
        YmlSerializer annotation = field.getAnnotation(YmlSerializer.class);
        if (annotation == null) {
            return true;
        }

        return false;
    }

    /**
     * filter ignore field
     */
    public static Boolean ignoreField(Field field) {
        YmlSerializer annotation = field.getAnnotation(YmlSerializer.class);
        if (annotation.ignore()) {
            return true;
        }

        return false;
    }

    /**
     * filter annotation fields
     *
     * @return Map<name, field>
     */
    public static Map<String, Field> filterAnnotationField(Class<?> clazz) {
        Map<String, Field> fieldMap = new HashMap<>();
        allFields(clazz).forEach((s, field) -> {
            YmlSerializer annotation = field.getAnnotation(YmlSerializer.class);
            if (annotation != null) {
                fieldMap.put(s, field);
            }
        });
        return fieldMap;
    }

    /**
     * get all fields
     *
     * @return Map<name, field>
     */
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

}
