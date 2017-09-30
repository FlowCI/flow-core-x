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

package com.flow.platform.util.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author yang
 */
public class HttpResponse<T> {

    final static int EXCEPTION_STATUS_CODE = -1;

    /**
     * Http response code
     */
    private final int statusCode;

    private final List<Throwable> exceptions;

    private final T body;

    private final int retried;

    HttpResponse(int retried, int statusCode, List<Throwable> exceptions, T body) {
        this.retried = retried;
        this.statusCode = statusCode;
        this.body = body;
        this.exceptions = exceptions == null ? Collections.unmodifiableList(new ArrayList<>(0))
            : Collections.unmodifiableList(exceptions);
    }

    public int getRetried() {
        return retried;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public List<Throwable> getExceptions() {
        return exceptions;
    }

    public boolean hasException() {
        return statusCode == EXCEPTION_STATUS_CODE;
    }

    public T getBody() {
        return body;
    }
}
