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

package com.flow.platform.api.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gyfirim
 */
public class YmlUtil
{

    public static Class<?> getOrigin() {
        return origin;
    }

    private static Class<?> origin;

    public static Class<?> set(Class<?> a){
        origin = a;
        return a;
    }

    public static <T> T test(Class<T> t){
        Object o = null;
        List<T> arrayList = new ArrayList<>();
        try {
            Constructor<?> declaredConstructor = t.getDeclaredConstructor();
            o = declaredConstructor.newInstance();
            arrayList.add((T) o);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return (T) o;
    }
}
