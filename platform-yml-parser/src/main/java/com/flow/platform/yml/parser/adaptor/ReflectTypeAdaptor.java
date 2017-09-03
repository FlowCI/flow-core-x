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

import com.flow.platform.yml.parser.factory.BaseFactory;
import com.flow.platform.yml.parser.util.ClazzUtil;
import com.flow.platform.yml.parser.util.TypeToken;

/**
 * @author yh@firim
 */
public class ReflectTypeAdaptor<E> extends BaseAdaptor<Object> {

    public final static BaseFactory FACTORY = new BaseFactory() {

        @Override
        public <T> BaseAdaptor<T> create(TypeToken<T> token) {
            Class<?> type = token.getRawType();

            if (!Object.class.isAssignableFrom(type)) {
                return null;
            }
            return new ReflectTypeAdaptor(type);
        }
    };

    private Class<E> componentClazz;

    public ReflectTypeAdaptor(Class<E> componentClazz) {
        this.componentClazz = componentClazz;
    }

    @Override
    public Object read(Object o) {
        // 假如o是Clazz的子类就直接赋值就行了
        if(componentClazz.isAssignableFrom(o.getClass())){
            return o;
        }
        return ClazzUtil.build(o, componentClazz);
    }

    @Override
    public void write(Object o, Object object) {
    }
}
