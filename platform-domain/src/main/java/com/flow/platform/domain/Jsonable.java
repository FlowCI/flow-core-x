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

package com.flow.platform.domain;

import com.flow.platform.domain.json.RuntimeTypeAdapterFactory;
import com.flow.platform.domain.json.ZonedDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * @author gy@fir.im
 */
public abstract class Jsonable implements Serializable {

    public final static RuntimeTypeAdapterFactory<Jsonable> RUNTIME_TYPE_ADAPTER_FACTORY =
        RuntimeTypeAdapterFactory.of(Jsonable.class);

    public final static Gson GSON_CONFIG = new GsonBuilder()
        .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter())
        .registerTypeAdapterFactory(RUNTIME_TYPE_ADAPTER_FACTORY)
        .create();

    public static <T extends Jsonable> T parse(String json, Class<T> tClass) {
        return GSON_CONFIG.fromJson(json, tClass);
    }

    public static <T extends Jsonable> T[] parseArray(String json, Class<T[]> tClass) {
        return GSON_CONFIG.fromJson(json, tClass);
    }

    public static <T extends Jsonable> T parse(byte[] bytes, Class<T> tClass) {
        return GSON_CONFIG.fromJson(new String(bytes), tClass);
    }

    public static <T extends Jsonable> T[] parseArray(byte[] bytes, Class<T[]> tClass) {
        return GSON_CONFIG.fromJson(new String(bytes), tClass);
    }

    public String toJson() {
        return GSON_CONFIG.toJson(this);
    }

    public byte[] toBytes() {
        return toJson().getBytes();
    }
}
