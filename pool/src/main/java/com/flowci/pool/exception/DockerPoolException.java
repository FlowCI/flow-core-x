
/*
 * Copyright 2020 flow.ci
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

package com.flowci.pool.exception;

import com.github.dockerjava.api.exception.DockerException;

import static java.text.MessageFormat.format;

public class DockerPoolException extends Exception {
    
    public DockerPoolException(final String message, final String... params) {
        super(format(message, params));
    }

    public DockerPoolException(DockerException e) {
        super(e.getMessage());
    }
}