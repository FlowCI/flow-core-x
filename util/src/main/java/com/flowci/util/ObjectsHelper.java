/*
 * Copyright 2018 flow.ci
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

package com.flowci.util;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author yang
 */
public abstract class ObjectsHelper {

    public static boolean hasCollection(Collection<?> collection) {
        return collection != null && collection.size() > 0;
    }

    public static boolean hasCollection(Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }

    public static Set<String> fields(Class<?> klass) {
        Set<String> fields = new HashSet<>();
        for (Class<?> c = klass; c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                fields.add(f.getName());
            }
        }
        return fields;
    }

    /**
     * Check input fields are all have value or not
     */
    public static <T> boolean hasValue(T instance, Set<String> fields) throws ReflectiveOperationException {
        Map<String, Field> all = new HashMap<>();

        for (Class<?> c = instance.getClass(); c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                all.put(f.getName(), f);
            }
        }

        for (String name : fields) {
            Field field = all.get(name);
            if (field == null) {
                throw new NoSuchFieldException(name);
            }

            field.setAccessible(true);
            Object o = field.get(instance);

            if (o == null) {
                return false;
            }

            if (o instanceof Collection) {
                if (((Collection<?>) o).isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    public static <T extends Serializable> List<T> copy(List<T> source) {
        List<T> dest = new ArrayList<>(source.size());

        for (T item : source) {
            dest.add(copy(item));
        }

        return dest;
    }

    public static <T extends Serializable> T copy(T source) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(source);
                oos.flush();
            }

            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
                return (T) ois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static boolean tryParseInt(String val) {
        try {
            Integer.parseInt(val);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static <T> void ifNotNull(T val, Consumer<T> consumer) {
        if (Objects.isNull(val)) {
            return;
        }

        consumer.accept(val);
    }

    public static void throwIfNotNull(RuntimeException e) {
        if (e != null) {
            throw e;
        }
    }
}
