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
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yh@firim
 */
public class MethodUtil {

    // 必填的字段
    public static Boolean requiredMethod(Method method) {
        YmlSerializer annotation = method.getAnnotation(YmlSerializer.class);
        if (annotation == null) {
            return false;
        }

        if (annotation.required() == true) {
            return true;
        }

        return false;
    }


    // get annotation's method
    public static Map<String, Method> filterAnnotationMethods(Class<?> clazz) {
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
