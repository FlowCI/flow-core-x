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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

/**
 * Gson object to json converter
 * - enable expose annotation
 * - convert ZonedDateTime to timestamp
 *
 * @author yh@firim
 */
public class GsonHttpExposeConverter extends GsonHttpMessageConverter {

    private Gson gsonForObjectToJson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdaptor())
        .create();

    public GsonHttpExposeConverter() {
        super();
    }

    @Override
    protected void writeInternal(Object o, Type type, HttpOutputMessage outputMessage)
        throws IOException, HttpMessageNotWritableException {

        Charset charset = getCharset(outputMessage.getHeaders());
        OutputStreamWriter writer = new OutputStreamWriter(outputMessage.getBody(), charset);
        try {
            if (type != null) {
                gsonForObjectToJson.toJson(o, type, writer);
            } else {
                gsonForObjectToJson.toJson(o, writer);
            }
            writer.close();
        } catch (JsonIOException ex) {
            throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
        }
    }

    private Charset getCharset(HttpHeaders headers) {
        if (headers == null || headers.getContentType() == null || headers.getContentType().getCharset() == null) {
            return DEFAULT_CHARSET;
        }
        return headers.getContentType().getCharset();
    }

    /**
     * Used for convert zoned date time to timestamp
     */
    private class ZonedDateTimeAdaptor extends TypeAdapter<ZonedDateTime> {

        @Override
        public void write(JsonWriter out, ZonedDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.toEpochSecond());
        }

        @Override
        public ZonedDateTime read(JsonReader in) throws IOException {
            Long ts = in.nextLong();
            Instant i = Instant.ofEpochSecond(ts);
            ZonedDateTime z;
            z = ZonedDateTime.ofInstant(i, ZoneId.systemDefault());
            return z;
        }
    }
}
