/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.common.helper;

import com.flowci.exception.ArgumentException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

/**
 * @author yang
 */
public abstract class DateHelper {

    private static final SimpleDateFormat intDayFormatter = new SimpleDateFormat("yyyyMMdd");

    public static synchronized int toIntDay(Date date) {
        return Integer.parseInt(intDayFormatter.format(date));
    }

    public static synchronized Instant toInstant(int day) {
        try {
            Date date = intDayFormatter.parse("" + day);
            return date.toInstant();
         } catch (ParseException e) {
            throw new ArgumentException("Invalid day format");
        }
    }
}
