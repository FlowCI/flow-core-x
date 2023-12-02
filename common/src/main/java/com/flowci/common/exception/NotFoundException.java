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

package com.flowci.common.exception;

/**
 * @author yang
 */
public class NotFoundException extends CIException {

    public NotFoundException(String message, String... params) {
        super(message, params);
    }

    public NotFoundException(String message, Throwable cause, String... params) {
        super(message, cause, params);
    }

    @Override
    public Integer getCode() {
        return ErrorCode.NOT_FOUND;
    }
}
