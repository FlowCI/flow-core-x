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

package com.flow.platform.yml.parser.test.adaptor;

import com.flow.platform.yml.parser.adaptor.BaseAdaptor;
import java.util.Collection;

/**
 * @author yh@firim
 */
public class StringAdaptor  extends BaseAdaptor<String>{

    @Override
    public String read(Object o) {
        StringBuilder stringBuilder = new StringBuilder("");
        ((Collection)o).forEach(action -> {
            stringBuilder.append(action);
        });
        return stringBuilder.toString();
    }

    @Override
    public Object write(String s) {
        return null;
    }
}
