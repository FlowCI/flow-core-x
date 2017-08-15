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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * @author gy@fir.im
 */
public abstract class Jsonable implements Serializable {

    public final static DateTimeFormatter DOMAIN_DATE_FORMAT = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss:SSSZ");

    public final static Gson GSON_CONFIG = new GsonBuilder()
        .registerTypeAdapter(ZonedDateTime.class, new TypeAdapter<ZonedDateTime>() {
            @Override
            public void write(JsonWriter writer, ZonedDateTime value) throws IOException {
                if (value != null) {
                    writer.value(value.format(DOMAIN_DATE_FORMAT));
                } else {
                    writer.nullValue();
                }
            }

            @Override
            public ZonedDateTime read(JsonReader reader) throws IOException {
                String raw = reader.nextString();
                try {
                    return ZonedDateTime.parse(raw, DOMAIN_DATE_FORMAT);
                } catch (DateTimeParseException ignored) {
                    return null;
                }
            }
        })
        .create();

    public static <T extends Jsonable> T parse(String json, Class<T> tClass) {
        return GSON_CONFIG.fromJson(json, tClass);
    }

    public static <T extends Jsonable> T parse(byte[] bytes, Class<T> tClass) {
        return GSON_CONFIG.fromJson(new String(bytes), tClass);
    }

    public String toJson() {
        return GSON_CONFIG.toJson(this);
    }

    public byte[] toBytes() {
        return toJson().getBytes();
    }
}
