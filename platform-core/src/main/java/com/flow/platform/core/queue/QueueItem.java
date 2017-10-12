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

/**
 * @author yang
 */
public final class QueueItem<T> {

    private final T data;

    private final int order;

    public QueueItem(T data) {
        this.data = data;
        this.order = 0;
    }

    public QueueItem(T data, Integer order) {
        this.data = data;
        this.order = order;
    }

    public T getData() {
        return data;
    }

    public int getOrder() {
        return order;
    }
}
