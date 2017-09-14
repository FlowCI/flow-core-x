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

package com.flow.platform.core.http.converter;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

/**
 * Ignore type just parse raw object, it is better for generic instance
 *
 * @author yang
 */
public class RawGsonMessageConverter extends GsonHttpMessageConverter {

    private boolean ignoreType;

    private Gson gsonForWriter;

    private Gson gsonForReader;

    public RawGsonMessageConverter() {
        super();
    }

    public RawGsonMessageConverter(boolean ignoreType, Gson gsonForWriter, Gson gsonForReader) {
        super();
        this.ignoreType = ignoreType;
        this.gsonForWriter = gsonForWriter;
        this.gsonForReader = gsonForReader;
    }

    private Charset getCharset(HttpHeaders headers) {
        if (headers == null || headers.getContentType() == null || headers.getContentType().getCharset() == null) {
            return DEFAULT_CHARSET;
        }
        return headers.getContentType().getCharset();
    }

    /**
     * Ignore type when convert object to json to get real polymorphic class json data
     */
    public void setIgnoreType(boolean ignoreType) {
        this.ignoreType = ignoreType;
    }

    @Override
    public void setGson(Gson gson) {
        // ignore setGson method
    }

    /**
     * Set gson for writer which convert from object to json
     */
    public void setGsonForWriter(Gson gsonForWriter) {
        this.gsonForWriter = gsonForWriter;
    }

    /**
     * Set gson for reader which convert from json to object
     */
    public void setGsonForReader(Gson gsonForReader) {
        this.gsonForReader = gsonForReader;
    }

    @Override
    protected void writeInternal(Object o, Type type, HttpOutputMessage outputMessage)
        throws IOException, HttpMessageNotWritableException {
        Charset charset = getCharset(outputMessage.getHeaders());

        try (OutputStreamWriter writer = new OutputStreamWriter(outputMessage.getBody(), charset)) {
            if (ignoreType) {
                gsonForWriter.toJson(o, writer);
                return;
            }

            if (type != null) {
                gsonForWriter.toJson(o, type, writer);
                return;
            }

            gsonForWriter.toJson(o, writer);
        } catch (JsonIOException ex) {
            throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
        }
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
        throws IOException, HttpMessageNotReadableException {
        TypeToken<?> token = getTypeToken(clazz);

        try (Reader json = new InputStreamReader(inputMessage.getBody(), getCharset(inputMessage.getHeaders()))) {
            return this.gsonForReader.fromJson(json, token.getType());
        } catch (JsonParseException ex) {
            throw new HttpMessageNotReadableException("JSON parse error: " + ex.getMessage(), ex);
        }
    }
}
