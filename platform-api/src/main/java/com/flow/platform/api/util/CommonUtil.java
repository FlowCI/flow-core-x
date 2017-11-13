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

import com.flow.platform.api.config.AppConfig;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

/**
 * @author yh@firim
 */
public class CommonUtil {

    /**
     * random ordered id
     *
     * @return long
     */
    public synchronized static BigInteger randomId() {
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmmssSSS");
        StringBuilder stringBuilder = new StringBuilder(localDateTime.format(formatter));
        String uuid = String.format("%08d", Math.abs(UUID.randomUUID().hashCode()) / 100);
        return new BigInteger(stringBuilder.append(uuid).toString());
    }


    /**
     * read commonsMultipartFile content to String
     * @param file
     * @return
     */
    public static String commonsMultipartFileToString(MultipartFile file) {
        try {
            InputStream is = file.getInputStream();
            return IOUtils.toString(is, AppConfig.DEFAULT_CHARSET.name());
        } catch (Throwable throwable) {
            return null;
        }
    }

}
