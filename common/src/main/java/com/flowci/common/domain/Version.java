/*
 * Copyright 2018 flow.ci
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

package com.flowci.common.domain;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Version class to handle the format {main}.{sub}.{update}.{build}
 *
 * Ex: 1.0.2 | 1.0.2.12312
 *
 * @author yang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@JsonSerialize(using = Version.Serializer.class)
@JsonDeserialize(using = Version.Deserializer.class)
public class Version implements Comparable<Version> {

    public static class Serializer extends JsonSerializer<Version> {

        @Override
        public void serialize(Version value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.toString());
        }
    }

    public static class Deserializer extends JsonDeserializer<Version> {

        @Override
        public Version deserialize(JsonParser p, DeserializationContext context) throws IOException {
            return Version.parse(p.getValueAsString());
        }
    }

    public static Version parse(String str) {
        Objects.requireNonNull(str, "Version string cannot be null");
        String[] items = str.split(DotPattern);

        if (items.length < 3 || items.length > 4) {
            throw new IllegalArgumentException("Illegal version string");
        }

        try {
            Version v = new Version();
            v.main = Integer.parseInt(items[0]);
            v.sub = Integer.parseInt(items[1]);
            v.update = Integer.parseInt(items[2]);

            if (items.length == 4) {
                v.build = Integer.parseInt(items[3]);
            }

            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The version item must be number");
        }
    }

    private static final String Dot = ".";

    private static final String DotPattern = "\\.";

    private Integer main = 0;

    private Integer sub = 0;

    private Integer update = 0;

    /**
     * Optional
     */
    private Integer build;

    @Override
    public int compareTo(Version o) {
        int compareOnMain = main.compareTo(o.main);
        if (compareOnMain != 0) {
            return compareOnMain;
        }

        int compareOnSub = sub.compareTo(o.sub);
        if (compareOnSub != 0) {
            return compareOnSub;
        }

        int compareOnUpdate = update.compareTo(o.update);
        if (compareOnUpdate != 0) {
            return compareOnUpdate;
        }

        if (Objects.isNull(build) && Objects.isNull(o.build)) {
            return 0;
        }

        if (Objects.isNull(build)) {
            return -1;
        }

        if (Objects.isNull(o.build)) {
            return 1;
        }

        return build.compareTo(o.build);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return compareTo((Version) o) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(main, sub, update, build);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(15);
        builder.append(main).append(Dot);
        builder.append(sub).append(Dot);
        builder.append(update);

        if (Objects.isNull(build)) {
            return builder.toString();
        }

        builder.append(Dot).append(build);
        return builder.toString();
    }
}
