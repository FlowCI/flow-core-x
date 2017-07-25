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

package com.flow.platform.api.util;

import com.flow.platform.util.Logger;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * @author yh@firim
 */
public class UrlUtil {

    private final static Logger LOGGER = new Logger(UrlUtil.class);

    /**
     * url encode
     */
    public static String urlEncoder(String s) {
        try {
            s = URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("url encode UnsupportedEncodingException %s", e);
            s = null;
        } catch (NullPointerException e) {
            LOGGER.error("url encode NullPointerException %s", e);
            s = null;
        }
        return s;
    }

    /**
     * url decode
     */
    public static String urlDecoder(String s) {
        try {
            s = URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn("url decode UnsupportedEncodingException %s", e);
            s = null;
        } catch (NullPointerException e) {
            LOGGER.warn("url decode NullPointerException %s", e);
            s = null;
        }
        return s;
    }
}
