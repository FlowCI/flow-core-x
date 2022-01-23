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

package com.flowci.core.githook.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.githook.domain.GitTriggerable;
import com.flowci.core.githook.domain.GitTrigger;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * To convert event from git to standard GitTrigger object
 *
 * @author yang
 */
@Log4j2
public abstract class TriggerConverter {

    @Autowired
    protected ObjectMapper objectMapper;

    public Optional<GitTrigger> convert(String event, InputStream body) {
        try {
            Map<String, Function<InputStream, GitTrigger>> mapping = getMapping();
            GitTrigger trigger = mapping.get(event).apply(body);
            return Optional.ofNullable(trigger);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    abstract GitSource getGitSource();

    // mappings for event - converter
    abstract Map<String, Function<InputStream, GitTrigger>> getMapping();

    class EventConverter<T extends GitTriggerable> implements Function<InputStream, GitTrigger> {

        private final String eventName;

        private final Class<T> target;

        EventConverter(String eventName, Class<T> target) {
            this.eventName = eventName;
            this.target = target;
        }

        @Override
        public GitTrigger apply(InputStream stream) {
            try {
                T event = objectMapper.readValue(stream, target);
                return event.toTrigger();
            } catch (IOException e) {
                log.warn("Unable to parse {} event for {}", eventName, getGitSource());
                return null;
            }
        }
    }
}
