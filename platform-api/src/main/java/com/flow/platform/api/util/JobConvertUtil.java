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

import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.NodeStatus;
import com.flow.platform.domain.Jsonable;
import com.google.common.base.Strings;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author yh@firim
 */
public class JobConvertUtil {

    public static Job convert(Object[] objects) {

        return new Job(((BigDecimal) objects[0]).toBigInteger(),
            (Integer) objects[1],
            (String) objects[2],
            (String) objects[3],
            (String) objects[4],
            tsToZoneDateTime((Timestamp) objects[5]),
            tsToZoneDateTime((Timestamp) objects[6]),
            convertStatus((String) objects[7]),
            convertInteger(objects[8]),
            Jsonable.GSON_CONFIG.fromJson((String) objects[9], Map.class),
            (convertBigInteger(objects[10])).longValue(),
            (String) objects[11],
            tsToZoneDateTime((Timestamp) objects[12]),
            tsToZoneDateTime((Timestamp) objects[13]));
    }

    private static ZonedDateTime tsToZoneDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        Instant i = timestamp.toInstant();
        ZonedDateTime z;
        z = ZonedDateTime.ofInstant(i, ZoneId.systemDefault());
        return z;
    }

    private static NodeStatus convertStatus(String name) {
        if (Strings.isNullOrEmpty(name)) {
            return NodeStatus.PENDING;
        }
        return NodeStatus.valueOf(name);
    }

    private static Integer convertInteger(Object object){
        if(object == null){
            return null;
        }
        return (Integer)object;
    }

    private static BigInteger convertBigInteger(Object object){
        if(object == null){
            return new BigInteger("0");
        }
        return (BigInteger)object;
    }

    public static List<Job> convert(List<Object[]> objects) {
        List<Job> jobs = new ArrayList<>();
        for (Object[] object : objects) {
            jobs.add(convert(object));
        }

        return jobs;
    }
}
