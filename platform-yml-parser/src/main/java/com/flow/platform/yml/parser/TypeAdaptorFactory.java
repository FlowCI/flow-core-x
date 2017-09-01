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

package com.flow.platform.yml.parser;

import com.flow.platform.yml.parser.adaptor.ArrayAdaptor;
import com.flow.platform.yml.parser.adaptor.CollectionAdaptor;
import com.flow.platform.yml.parser.adaptor.PrimitiveAdapor;
import com.flow.platform.yml.parser.adaptor.ReflectTypeAdaptor;
import com.flow.platform.yml.parser.adaptor.TypeAdaptor;
import com.flow.platform.yml.parser.annotations.YmlSerializer;

/**
 * @author gyfirim
 */
public class TypeAdaptorFactory {

    public static TypeAdaptor getTypeAdaptor(YmlSerializer ymlSerializer) {

        if (ymlSerializer.isPrimitive()) {
            return new PrimitiveAdapor();
        } else {
            try {
                return (TypeAdaptor) ymlSerializer.adaptor().newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static <T> TypeAdaptor getTypeAdaptor(T t){
        if(((Class) t).isArray()){
            return new ArrayAdaptor(((Class<?>)t).getComponentType());
        }
        if(Object.class.isAssignableFrom((Class<?>) t)){
            return new ReflectTypeAdaptor();
        }
        return null;
    }
}
