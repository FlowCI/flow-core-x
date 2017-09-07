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

import com.flow.platform.domain.Jsonable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

/**
 * Credential content wrapper class
 *
 * @author yang
 */
@JsonAdapter(value = CredentialDetail.JsonAdapter.class)
public class CredentialDetail extends Jsonable {

    /**
     * It is duplicate with Credential, used for json parsing
     */
    protected CredentialType type;

    public CredentialType getType() {
        return type;
    }

    public static class JsonAdapter extends TypeAdapter<CredentialDetail> {

        @Override
        public void write(JsonWriter out, CredentialDetail value) throws IOException {
            out.jsonValue(value.toJson());
        }

        @Override
        public CredentialDetail read(JsonReader in) throws IOException {
            JsonElement element = Streams.parse(in);

            if (!element.isJsonObject()) {
                return null;
            }

            JsonObject jsonObject = (JsonObject) element;
            String typeStr = jsonObject.getAsJsonPrimitive("type").getAsString();

            Class<? extends CredentialDetail> clazz = CredentialType.valueOf(typeStr).getClazz();
            return GSON_CONFIG.fromJson(jsonObject, clazz);
        }
    }
}
