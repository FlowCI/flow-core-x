/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.common.rabbit;

import com.rabbitmq.client.Connection;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

/**
 * Create channel and handle the operation on the channel
 * Allowed to handle multiple queues in one channel
 * enable to declare, delete, purge queue, start and stop consumer on queue
 *
 * @author yang
 */
@Log4j2
@Getter
public final class RabbitChannelOperation extends RabbitOperation {

    public RabbitChannelOperation(Connection conn, Integer concurrency, String name) throws IOException {
        super(conn, concurrency, name);
    }

    @Override
    public void close() throws Exception {
        super.close();
        log.debug("[Close] RabbitChannelManager {} will be closed", name);
    }
}
