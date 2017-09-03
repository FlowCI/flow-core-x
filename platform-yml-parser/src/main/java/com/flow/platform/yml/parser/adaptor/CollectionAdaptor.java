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

package com.flow.platform.yml.parser.adaptor;

import com.flow.platform.yml.parser.TypeAdaptorFactory;
import com.flow.platform.yml.parser.factory.BaseFactory;
import com.flow.platform.yml.parser.util.$Gson$Types;
import com.flow.platform.yml.parser.util.ClazzUtil;
import com.flow.platform.yml.parser.util.TypeToken;
import com.sun.xml.internal.rngom.parse.host.Base;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author yh@firim
 */
public class CollectionAdaptor<E> extends BaseAdaptor<Object> {

    public final static BaseFactory FACTORY = new BaseFactory() {

        @Override
        public <T> BaseAdaptor<T> create(TypeToken<T> typeToken) {
            Type type = typeToken.getType();
            Class<? super T> rawType = typeToken.getRawType();
            if(!Collection.class.isAssignableFrom(rawType)){
                return null;
            }

            Type elementType = $Gson$Types.getCollectionElementType(type, rawType);
            BaseAdaptor<?> elementTypeAdapter = TypeAdaptorFactory.getAdaptor(TypeToken.get(elementType));

            return new CollectionAdaptor(rawType, elementTypeAdapter);
        }
    };


    private Class<E> componentClazz;

    private BaseAdaptor<E> typeAdaptor;

    public CollectionAdaptor(Class<E> componentClazz, BaseAdaptor<E> typeAdaptor) {
        this.componentClazz = componentClazz;
        this.typeAdaptor = typeAdaptor;
    }

    @Override
    public Object read(Object o) {
        List<E> list = new ArrayList<>();

//        ((Collection) o).forEach(action -> {
//            list.add(ClazzUtil.build(action, componentClazz));
//        });

        ((Collection) o).forEach(action -> {
            list.add(typeAdaptor.read(action));
        });

        return list;
    }

    @Override
    public void write(Object o, Object object) {

    }
}
