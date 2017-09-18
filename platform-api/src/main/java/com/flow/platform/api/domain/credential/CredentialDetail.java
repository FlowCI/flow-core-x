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

package com.flow.platform.api.domain.credential;

import com.flow.platform.api.domain.credential.CredentialDetail.DetailSerializer;
import com.flow.platform.domain.Jsonable;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import java.lang.reflect.Type;

/**
 * Credential content wrapper class
 *
 * @author yang
 */
@JsonAdapter(value = DetailSerializer.class)
public class CredentialDetail extends Jsonable {

    /**
     * It is duplicate with Credential, used for json parsing
     */
    @Expose
    protected CredentialType type;

    public CredentialType getType() {
        return type;
    }

    public static class DetailSerializer
        implements JsonSerializer<CredentialDetail>, JsonDeserializer<CredentialDetail> {

        @Override
        public JsonElement serialize(CredentialDetail src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src, src.getClass());
        }

        @Override
        public CredentialDetail deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

            if (!element.isJsonObject()) {
                return null;
            }

            JsonObject jsonObject = (JsonObject) element;
            String typeStr = jsonObject.getAsJsonPrimitive("type").getAsString();
            Class<? extends CredentialDetail> clazz = CredentialType.valueOf(typeStr).getClazz();
            return context.deserialize(element, clazz);
        }
    }
}
