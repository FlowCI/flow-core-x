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
import java.util.Locale;

/**
 * @author yang
 */
public class SystemUtil {

    private final static String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

    public static boolean isWindows() {
        return OS_NAME.startsWith("win");
    }

    public static boolean isWindows(String os) {
        if (Strings.isNullOrEmpty(os)) {
            return false;
        }
        return os.toLowerCase(Locale.ENGLISH).startsWith("win");
    }
}
