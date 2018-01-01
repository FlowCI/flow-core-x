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

package com.flow.platform.queue;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * @author yang
 */
public interface PriorityQueueItem extends Serializable, Comparable<PriorityQueueItem> {

    ItemComparator COMPARATOR = new ItemComparator();

    Long getPriority();

    Long getTimestamp();

    byte[] getBody();

    class ItemComparator implements Comparator<PriorityQueueItem> {

        @Override
        public int compare(PriorityQueueItem o1, PriorityQueueItem o2) {
            if (Objects.equals(o1.getPriority(), o2.getPriority())) {
                return o2.getTimestamp().compareTo(o1.getTimestamp());
            }

            return o2.getPriority().compareTo(o1.getPriority());
        }
    }
}
