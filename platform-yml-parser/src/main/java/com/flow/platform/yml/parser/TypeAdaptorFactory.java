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
import com.flow.platform.yml.parser.adaptor.BaseAdaptor;
import com.flow.platform.yml.parser.adaptor.CollectionAdaptor;
import com.flow.platform.yml.parser.adaptor.MapAdaptor;
import com.flow.platform.yml.parser.adaptor.PrimitiveAdaptor;
import com.flow.platform.yml.parser.adaptor.ReflectTypeAdaptor;
import com.flow.platform.yml.parser.exception.YmlParserException;
import com.flow.platform.yml.parser.factory.BaseFactory;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yh@firim
 */
public class TypeAdaptorFactory {

    public static List<BaseFactory> factories = new ArrayList<>();

    static {
        factories.add(MapAdaptor.FACTORY);
        factories.add(PrimitiveAdaptor.FACTORY);
        factories.add(CollectionAdaptor.FACTORY);
        factories.add(ArrayAdaptor.FACTORY);
        factories.add(ReflectTypeAdaptor.FACTORY);
    }


    public static BaseAdaptor getAdaptor(Type type) {
        for (BaseFactory factory : factories) {
            BaseAdaptor adaptor = factory.create(type);
            if (adaptor != null) {
                return adaptor;
            }
        }

        throw new YmlParserException("Not found adaptor");
    }

}
