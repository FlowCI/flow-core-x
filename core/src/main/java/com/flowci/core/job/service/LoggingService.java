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

package com.flowci.core.job.service;

import com.flowci.domain.ExecutedCmd;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author yang
 */
public interface LoggingService {

    /**
     * To receive the message from amqp
     */
    void handleLoggingItem(String message);

    /**
     * To read logs from file store
     */
    Page<String> read(ExecutedCmd cmd, Pageable pageable);

    /**
     * Save log into file system, return the id of file
     *
     * @param fileName {cmd id}.log
     * @param stream file stream
     */
    String save(String fileName, InputStream stream) throws IOException;

    /**
     * Get log resource
     *
     * @param cmdId cmd id
     * @return file resource
     */
    Resource get(String cmdId);

}
