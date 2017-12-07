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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * @author gy@fir.im
 */
public class DateUtil {

    public final static ZoneId ZONE_UTC = ZoneId.of("UTC");

    public static ZonedDateTime fromDateForUTC(Date date) {
        Instant instant = date.toInstant();
        return instant.atZone(ZONE_UTC);
    }

    public static Date toDate(ZonedDateTime zonedDateTime) {
        return Date.from(zonedDateTime.toInstant());
    }

    public static ZonedDateTime toUtc(ZonedDateTime zonedDateTime) {
        if (zonedDateTime.getZone() != ZONE_UTC) {
            return zonedDateTime.withZoneSameInstant(DateUtil.ZONE_UTC);
        }
        return zonedDateTime;
    }

    public static ZonedDateTime utcNow() {
        return ZonedDateTime.now(ZONE_UTC);
    }

    public static ZonedDateTime now() {
        try {
            return ZonedDateTime.now(ZoneId.systemDefault());
        } catch (Throwable e) {
            return utcNow();
        }
    }

    public static boolean isTimeOut(ZonedDateTime start, ZonedDateTime target, long timeOutSeconds) {
        if (start.getZone() != ZONE_UTC) {
            start = toUtc(start);
        }

        if (target.getZone() != ZONE_UTC) {
            target = toUtc(target);
        }

        final long runningInSeconds = ChronoUnit.SECONDS.between(start, target);
        return runningInSeconds >= timeOutSeconds;
    }
}
