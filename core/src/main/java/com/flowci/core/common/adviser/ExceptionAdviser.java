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

package com.flowci.core.common.adviser;

import com.flowci.core.common.domain.StatusCode;
import com.flowci.core.common.domain.http.ResponseMessage;
import com.flowci.common.exception.CIException;
import com.flowci.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author yang
 */
@Slf4j
@ControllerAdvice({
        "com.flowci.core.auth",
        "com.flowci.core.user",
        "com.flowci.core.flow",
        "com.flowci.core.job",
        "com.flowci.core.agent",
        "com.flowci.core.stats",
        "com.flowci.core.secret",
        "com.flowci.core.plugin",
        "com.flowci.core.config",
        "com.flowci.core.trigger",
        "com.flowci.core.api",
        "com.flowci.core.git",
        "com.flowci.core.common.controller"
})
public class ExceptionAdviser {

    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseMessage<Object> inputArgumentException(MethodArgumentNotValidException e) {
        String msg = e.getMessage();

        var fieldError = e.getBindingResult().getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null) {
            msg = fieldError.getDefaultMessage();
        }

        return new ResponseMessage<>(ErrorCode.INVALID_ARGUMENT, msg, null);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseMessage<Object> inputArgumentException(MissingServletRequestParameterException e) {
        return new ResponseMessage<>(ErrorCode.INVALID_ARGUMENT, e.getMessage(), null);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(CIException.class)
    public ResponseMessage<Object> ciException(CIException e) {
        return new ResponseMessage<>(e.getCode(), e.getMessage(), null);
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(Throwable.class)
    public ResponseMessage<Object> fatalException(Throwable e) {
        log.error("Fatal exception", e);
        return new ResponseMessage<>(StatusCode.FATAL, e.getMessage(), null);
    }
}
