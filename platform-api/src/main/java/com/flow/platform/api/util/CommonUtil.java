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

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * @author yh@firim
 */
public class CommonUtil {

    /**
     * random ordered id
     *
     * @return long
     */
    public static BigInteger randomId() {
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmmssSSS");
        StringBuilder stringBuilder = new StringBuilder(localDateTime.format(formatter));
        String uuid = String.format("%08d", Math.abs(UUID.randomUUID().hashCode()) / 100);
        BigInteger id = new BigInteger(stringBuilder.append(uuid).toString());
        return id;
    }
}
