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

import com.flow.platform.util.CollectionUtil;
import com.flow.platform.util.StringUtil;
import com.google.common.base.Strings;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.codec.binary.StringUtils;

/**
 * @author yang
 */
public class HttpURL {

    public final static String SLASH = "/";

    public static String encode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (Throwable ignore) {
            return null;
        }
    }

    public static String decode(String str) {
        try {
            return URLDecoder.decode(str, "UTF-8");
        } catch (Throwable ignore) {
            return null;
        }
    }


    public static HttpURL build(String url) {
        return new HttpURL(url);
    }

    private StringBuilder builder;

    private Map<String, String> params = new LinkedHashMap<>();

    private HttpURL(String url) {
        if (Strings.isNullOrEmpty(url)) {
            throw new IllegalArgumentException("Illegal url string");
        }
        builder = new StringBuilder(StringUtil.trimEnd(url, SLASH));
    }

    public HttpURL append(String path) {
        if (path == null || path.trim().isEmpty()) {
            return this;
        }

        builder.append(SLASH).append(StringUtil.trim(path, SLASH));
        return this;
    }

    public HttpURL withParam(String name, String value) {
        params.put(name, value);
        return this;
    }

    public String toString() {
        StringBuilder paramsBuilder = new StringBuilder();

        if (!CollectionUtil.isNullOrEmpty(params)) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                paramsBuilder.append("&")
                    .append(entry.getKey())
                    .append("=")
                    .append(encode(entry.getValue()));
            }

            paramsBuilder.deleteCharAt(0);
            builder.append("?").append(paramsBuilder);
        }

        return builder.toString();
    }

    public URL toURL() throws MalformedURLException {
        return new URL(toString());
    }
}
