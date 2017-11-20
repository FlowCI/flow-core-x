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

package com.flow.platform.core.queue;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/**
 * @author yang
 */
public class PriorityMessage extends Message implements Comparable<PriorityMessage> {

    public static PriorityMessage create(byte[] content, int priority) {
        MessageProperties properties = new MessageProperties();
        properties.setPriority(priority);
        return new PriorityMessage(content, properties);
    }

    public PriorityMessage(Message message) {
        super(message.getBody(), message.getMessageProperties());
    }

    public PriorityMessage(byte[] body, MessageProperties messageProperties) {
        super(body, messageProperties);
    }

    @Override
    public int compareTo(PriorityMessage o) {
        return o.getMessageProperties().getPriority().compareTo(this.getMessageProperties().getPriority());
    }
}
