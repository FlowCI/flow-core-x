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
import com.flowci.domain.http.ResponseMessage;
import com.flowci.exception.CIException;
import com.flowci.exception.ErrorCode;
import lombok.extern.log4j.Log4j2;
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
@Log4j2
@ControllerAdvice({
    "com.flowci.core.auth",
    "com.flowci.core.user",
    "com.flowci.core.flow",
    "com.flowci.core.job",
    "com.flowci.core.agent",
    "com.flowci.core.stats",
    "com.flowci.core.secret",
    "com.flowci.core.plugin",
    "com.flowci.core.config"
})
public class ExceptionAdviser {


    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        MissingServletRequestParameterException.class
    })
    public ResponseMessage<Object> inputArgumentException(Exception e) {
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
        log.error(e);
        return new ResponseMessage<>(StatusCode.FATAL, e.getMessage(), null);
    }
}
