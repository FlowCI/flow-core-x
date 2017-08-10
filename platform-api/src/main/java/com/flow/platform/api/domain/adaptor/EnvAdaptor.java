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

package com.flow.platform.api.domain.adaptor;

import com.flow.platform.api.domain.response.EnvWithDesc;
import com.flow.platform.api.util.I18nUtil;
import com.flow.platform.domain.Jsonable;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yh@firim
 */
public class EnvAdaptor extends TypeAdapter<Map<String, String>> {

    @Override
    public void write(JsonWriter out, Map<String, String> value) throws IOException {
        Map<String, EnvWithDesc> stringEnvWithDescMap = new HashMap<>();
        if (value != null) {
            value.forEach((k, v) -> {
                stringEnvWithDescMap.put(k, new EnvWithDesc(v, I18nUtil.translate(k)));
            });
        }
        out.jsonValue(Jsonable.GSON_CONFIG.toJson(stringEnvWithDescMap));
    }

    @Override
    public Map<String, String> read(JsonReader in) throws IOException {
        String str = in.nextString();
        Map<String, String> map = new HashMap<>();
        Map<String, EnvWithDesc> stringEnvWithDescMap = Jsonable.GSON_CONFIG
            .fromJson(str, Map.class);
        stringEnvWithDescMap.forEach((k, v) -> {
            map.put(k, v.getValue());
        });
        return map;
    }
}
