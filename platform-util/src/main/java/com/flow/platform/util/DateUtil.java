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

    public static Date utcNow() {
        return toDate(ZonedDateTime.now(ZONE_UTC));
    }
}
