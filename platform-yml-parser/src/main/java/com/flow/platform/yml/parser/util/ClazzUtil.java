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
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * @author yh@firim
 */
public class ClazzUtil {

    public static <T> T build(Object o, Class<T> clazz) {
        try {

            T instance = clazz.newInstance();

            //过滤带有注释的field
            FieldUtil.filterAnnotationField(clazz).forEach((s, field) -> {
                // 获取 field 对应的值
                YmlSerializer ymlSerializer = field.getAnnotation(YmlSerializer.class);

                Object obj = ((Map) o).get(getAnnotationMappingName(s, ymlSerializer));

                // 必须的属性
                if (FieldUtil.requiredField(field)) {
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
            MethodUtil.filterAnnotationMethods(clazz).forEach((s, method) -> {
                YmlSerializer ymlSerializer = method.getAnnotation(YmlSerializer.class);
                // 获取 field 对应的值
                Object obj = ((Map) o).get(getAnnotationMappingName(s, ymlSerializer));

                // 必须的属性
                if (MethodUtil.requiredMethod(method)) {
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
}