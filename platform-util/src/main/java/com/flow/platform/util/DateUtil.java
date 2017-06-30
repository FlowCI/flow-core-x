package com.flow.platform.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Created by gy@fir.im on 09/06/2017.
 * Copyright fir.im
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
}
