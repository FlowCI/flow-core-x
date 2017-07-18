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

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

/**
 * @author gy@fir.im
 */
public class ClassUtil {

    /**
     * Load jar from specific path
     *
     * @param loader specific class loader
     * @param packageName scan package name
     * @param classNameFilter nullable, class name filter, simplelly using contains
     * @param annotationFilter nullable, annotation type filter
     * @return set of classes or null when get error
     */
    public static Set<Class<?>> load(
        final ClassLoader loader,
        final String packageName,
        final Set<String> classNameFilter,
        final Set<Class<? extends Annotation>> annotationFilter) {

        ClassPath classPath;
        try {
            classPath = ClassPath.from(loader);
        } catch (IOException e) {
            return null;
        }

        ImmutableSet<ClassInfo> classSet = classPath.getTopLevelClassesRecursive(packageName);
        Set<Class<?>> classes = new HashSet<>(classSet.size());

        for (ClassInfo classInfo : classSet) {
            try {
                Class<?> aClass = classInfo.load();

                // check class name by filter
                if (classNameFilter != null) {
                    for (String filter : classNameFilter) {
                        if (aClass.toString().contains(filter)) {
                            classes.add(aClass);
                        }
                    }
                }

                // check annotation type by filter
                if (annotationFilter != null) {
                    Annotation[] annotations = aClass.getAnnotations();
                    for (Annotation annotation : annotations) {
                        for (Class<?> targetType : annotationFilter) {
                            if (annotation.annotationType().equals(targetType)) {
                                classes.add(aClass);
                            }
                        }
                    }
                }

                // add class if not filter defined
                if (classNameFilter == null && annotationFilter == null) {
                    classes.add(aClass);
                }

            } catch (NoClassDefFoundError ignore) {
                // no class found
            }
        }

        return classes;
    }
}
