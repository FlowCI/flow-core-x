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

import com.flow.platform.domain.Jsonable;
import com.google.gson.JsonIOException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

/**
 * @author yh@firim
 */
public class GsonHttpExposeConverter extends GsonHttpMessageConverter {

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
                Jsonable.GSON_EXPOSE_CONFIG.toJson(o, type, writer);
            } else {
                Jsonable.GSON_EXPOSE_CONFIG.toJson(o, writer);
            }
            writer.close();
        } catch (JsonIOException ex) {
            throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "GsonHttpExposeConverter{}";
    }

    private Charset getCharset(HttpHeaders headers) {
        if (headers == null || headers.getContentType() == null || headers.getContentType().getCharset() == null) {
            return DEFAULT_CHARSET;
        }
        return headers.getContentType().getCharset();
    }
}
