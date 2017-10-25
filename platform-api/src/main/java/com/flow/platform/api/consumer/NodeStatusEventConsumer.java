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

package com.flow.platform.api.consumer;

import com.flow.platform.api.domain.job.NodeResultKey;
import com.flow.platform.api.events.NodeStatusChangeEvent;
import com.flow.platform.util.Logger;
import org.springframework.context.ApplicationListener;

/**
 * @author yang
 */
public class NodeStatusEventConsumer extends JobEventPushHandler implements ApplicationListener<NodeStatusChangeEvent> {

    private final static Logger LOGGER = new Logger(NodeStatusEventConsumer.class);

    @Override
    public void onApplicationEvent(NodeStatusChangeEvent event) {
        NodeResultKey resultKey = event.getResultKey();
        LOGGER.debug("Node result %s status change event from %s to %s",
            resultKey.getPath(), event.getFrom(), event.getTo());
        System.out.println("onApplicationEvent:===================" + Thread.currentThread().getId());
        System.out.println(
            "onApplicationEvent:===================PATH:" + resultKey.getPath() + "# From:" + event.getFrom() + "# to:"
                + event.getTo());
        push(resultKey.getJobId());
    }
}
