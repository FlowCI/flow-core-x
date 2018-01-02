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

import com.flow.platform.queue.PriorityQueueItem;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/**
 * @author yang
 */
public class PriorityMessage extends Message implements PriorityQueueItem {

    private Long priority;

    private Long timestamp;

    public static PriorityMessage create(byte[] content, long priority) {
        MessageProperties properties = new MessageProperties();
        return new PriorityMessage(content, properties, priority);
    }

    public PriorityMessage(Message message) {
        super(message.getBody(), message.getMessageProperties());
        this.timestamp = System.nanoTime();
    }

    public PriorityMessage(byte[] body, MessageProperties messageProperties, long priority) {
        super(body, messageProperties);
        this.priority = priority;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public Long getPriority() {
        return this.priority;
    }

    public void setPriority(long priority) {
        this.priority = priority;
    }

    @Override
    public Long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(PriorityQueueItem o) {
        return COMPARATOR.compare(this, o);
    }
}
