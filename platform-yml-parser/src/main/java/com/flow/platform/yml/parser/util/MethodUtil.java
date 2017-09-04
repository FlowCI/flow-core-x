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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author yh@firim
 */
public class MethodUtil {

    private static String fieldNameForSetterGetter(String fieldName) {
        return Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    public static Type getClazz(Field field, Class<?> clazz) {
        Method method = getGetterMethod(field, clazz);
        if(method == null){
            return null;
        }

        return method.getGenericReturnType();
    }

    private static Method getGetterMethod(Field field, Class<?> clazz) {
        return getMethod(field, clazz, "get");
    }


    private static Method getSetterMethod(Field field, Class<?> clazz) {
        return getMethod(field, clazz, "set");
    }

    private static Method getMethod(Field field, Class<?> clazz, String action) {
        try {
            Method method = clazz
                .getDeclaredMethod(String.format("%s%s", action, fieldNameForSetterGetter(field.getName())));
            return method;
        } catch (Throwable throwable) {
            return null;
        }
    }
}
