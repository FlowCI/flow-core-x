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

package com.flow.platform.api.push;

import com.flow.platform.core.http.converter.RawGsonMessageConverter;
import com.flow.platform.domain.Jsonable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * @author yang
 */
public abstract class PushHandler {

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private RawGsonMessageConverter jsonConverter;

    public void push(String topic, Jsonable jsonable) {
        String objectInJson = jsonConverter.getGsonForWriter().toJson(jsonable);
        template.convertAndSend(topic, objectInJson);
    }

    public void push(String topic, String raw) {
        template.convertAndSend(topic, raw);
    }
}
