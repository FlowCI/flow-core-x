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

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author yh@firim
 */
public class I18nUtil {

    private static ResourceBundle resourceBundle = ResourceBundle.getBundle("i18n");

    public static void initLocale(String language, String countryCode) {
        Locale.setDefault(new Locale(language, countryCode));
        resourceBundle = ResourceBundle.getBundle("i18n");
    }

    public static String translate(String key) {
        try {
            return new String(resourceBundle.getString(key).getBytes("ISO-8859-1"), "UTF-8");
        } catch (Throwable throwable) {
            return null;
        }
    }
}
