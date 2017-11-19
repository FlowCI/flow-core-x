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

package com.flow.platform.plugin.util;

import com.flow.platform.plugin.exception.PluginException;
import com.flow.platform.util.http.HttpResponse;
import java.net.URI;
import java.util.Objects;
import org.apache.http.HttpStatus;

/**
 * @author yh@firim
 */
public class UriUtil {

    /**
     * detect route path
     * @param url
     * @return
     */
    public static String getRoutePath(String url) {
        URI uri = URI.create(url);
        return uri.getPath();
    }

    /**
     * detect response is ok, not ok throw exceptions
     * @param response
     */
    public static void detectResponseIsOKAndThrowable(HttpResponse<String> response) {
        // if not 200, show exception
        if (!Objects.equals(response.getStatusCode(), HttpStatus.SC_OK)) {
            throw new PluginException(String
                .format("status code is not 200, status code is %s, exception info is %s", response.getStatusCode(),
                    response.getExceptions()));
        }
    }

    /**
     * detect is github or not from url
     * @param url
     * @return
     */
    public static Boolean isGithubSource(String url) {
        URI uri = URI.create(url);
        return uri.getHost().contains("github.com");
    }
}
