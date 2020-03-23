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

package com.flowci.core.api.adviser;

import com.flowci.core.common.domain.StatusCode;
import com.flowci.domain.http.ResponseMessage;
import com.flowci.exception.CIException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author yang
 */
@ControllerAdvice({"com.flowci.core.api"})
public class ExceptionAdviceForAPI {

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(CIException.class)
    public ResponseMessage<Object> ciException(CIException e) {
        return new ResponseMessage<>(e.getCode(), e.getMessage(), null);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(Throwable.class)
    public ResponseMessage<Object> fatalException(Throwable e) {
        return new ResponseMessage<>(StatusCode.FATAL, e.getMessage(), null);
    }
}
