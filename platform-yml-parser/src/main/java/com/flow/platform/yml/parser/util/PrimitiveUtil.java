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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yh@firim
 */
public class PrimitiveUtil {

    private static final Set<Class> WRAPPER_TYPES = new HashSet(
        Arrays.asList(
            Boolean.class,
            Character.class,
            Byte.class,
            Short.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            Void.class
        ));

    private static final Set<Class> SPECIAL_TYPES = new HashSet(
        Arrays.asList(
            String.class
        ));


    public static boolean isWrapperType(Class clazz) {
        return WRAPPER_TYPES.contains(clazz);
    }

    /**
     * detect data type is primitive or not
     */
    public static boolean isUsePrimitive(Class clazz) {
        if (WRAPPER_TYPES.contains(clazz)) {
            return true;
        }

        if (SPECIAL_TYPES.contains(clazz)) {
            return true;
        }

        return false;
    }

}
