/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.common.helper;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.flowci.core.common.domain.JsonablePage;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

/**
 * @author yang
 */
public abstract class JacksonHelper {

    public static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SimpleModule module = new SimpleModule();
        module.addDeserializer(Pageable.class, new JsonablePage.PageableDeserializer());

        module.addSerializer(Instant.class, new DateHelper.InstantUTCSerializer());
        module.addDeserializer(Instant.class, new DateHelper.InstantUTCDeserializer());

        mapper.registerModule(module);

        return mapper;
    }

}
