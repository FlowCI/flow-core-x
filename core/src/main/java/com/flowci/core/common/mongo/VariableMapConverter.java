/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.common.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.domain.StringVars;
import com.flowci.domain.TypedVars;
import com.flowci.domain.Vars;
import com.flowci.common.exception.ArgumentException;
import java.io.IOException;
import java.util.Objects;
import lombok.Getter;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;

@Getter
public class VariableMapConverter {

    private final ObjectMapper objectMapper;

    private final Reader reader;

    private final StringVarWriter stringVarWriter;

    private final TypedVarWriter typedVarWriter;

    public VariableMapConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.reader = new Reader();
        this.stringVarWriter = new StringVarWriter();
        this.typedVarWriter = new TypedVarWriter();
    }

    public class Reader implements Converter<Document, Vars<?>> {

        @Override
        public Vars<?> convert(Document source) {
            try {
                String type = source.getString(Vars.JSON_TYPE_FIELD);

                if (Objects.isNull(type)) {
                    source.put(Vars.JSON_TYPE_FIELD, Vars.JSON_STRING_TYPE);
                    type = Vars.JSON_STRING_TYPE;
                }

                if (type.equals(Vars.JSON_STRING_TYPE)) {
                    return objectMapper.readValue(source.toJson(), StringVars.class);
                }

                if (type.equals(Vars.JSON_TYPED_TYPE)) {
                    return objectMapper.readValue(source.toJson(), TypedVars.class);
                }

                throw new ArgumentException("Missing type code for vars");

            } catch (IOException e) {
                throw new ArgumentException("Cannot parse mongo doc {0} to StringVars", source.toJson());
            }
        }
    }

    public class StringVarWriter implements Converter<StringVars, Document> {

        @Override
        public Document convert(StringVars src) {
            try {
                String json = objectMapper.writeValueAsString(src);
                return Document.parse(json);
            } catch (JsonProcessingException e) {
                throw new ArgumentException("Cannot parse StringVars to json");
            }
        }
    }

    public class TypedVarWriter implements Converter<TypedVars, Document> {

        @Override
        public Document convert(TypedVars src) {
            try {
                String json = objectMapper.writeValueAsString(src);
                return Document.parse(json);
            } catch (JsonProcessingException e) {
                throw new ArgumentException("Cannot parse StringVars to json");
            }
        }
    }
}
