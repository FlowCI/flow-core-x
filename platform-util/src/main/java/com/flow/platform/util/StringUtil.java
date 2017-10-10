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

package com.flow.platform.util;

import com.google.common.base.Strings;

/**
 * @author yang
 */
public class StringUtil {

    public final static String EMPTY = "";

    public static boolean isNullOrEmptyForItems(final String... strings) {
        if (CollectionUtil.isNullOrEmpty(strings)) {
            return true;
        }

        for (String item : strings) {
            if (item == null) {
                continue;
            }

            item = item.trim();
            if (!EMPTY.equals(item)) {
                return false;
            }
        }

        return true;
    }

    public static String trim(final String raw, final String trimStr) {
        String str = trimStart(raw, trimStr);
        return trimEnd(str, trimStr);
    }

    public static String trimEnd(final String raw, final String trimStr) {
        if (Strings.isNullOrEmpty(trimStr)) {
            return raw;
        }

        if (raw.endsWith(trimStr)) {
            return raw.substring(0, raw.length() - trimStr.length());
        }

        return raw;
    }

    public static String trimStart(final String raw, final String trimStr) {
        if (Strings.isNullOrEmpty(trimStr)) {
            return raw;
        }

        if (raw.startsWith(trimStr)) {
            return raw.substring(trimStr.length());
        }

        return raw;
    }
}
