package com.flow.platform.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by gy@fir.im on 13/07/2017.
 * Copyright fir.im
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
