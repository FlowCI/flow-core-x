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

package com.flowci.exception;

/**
 * @author yang
 */
public abstract class ErrorCode {

    public static final Integer ERROR = 400;

    public static final Integer AUTH_FAILURE = 401;

    public static final Integer INVALID_ARGUMENT = 402;

    public static final Integer NO_PERMISSION = 403;

    public static final Integer NOT_FOUND = 404;

    public static final Integer NOT_AVAILABLE = 405;

    public static final Integer DUPLICATE = 406;

    public static final Integer ILLEGAL_STATUS = 421;

    public static final Integer PARSE_YML_OR_JSON = 430;

}
