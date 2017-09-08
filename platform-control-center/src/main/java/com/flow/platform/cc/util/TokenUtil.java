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

package com.flow.platform.cc.util;

import java.security.MessageDigest;
import java.util.Arrays;
import org.apache.commons.codec.binary.Base64;

/**
 * @author yh@firim
 */
public class TokenUtil {

    public static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(s.getBytes());
            return Arrays.toString(md.digest());
        } catch (Throwable throwable) {
            return null;
        }
    }

    public static String encode(String s) {
        return Arrays.toString(Base64.encodeBase64(s.getBytes()));
    }

    public static String decode(String s) {
        return Arrays.toString(Base64.decodeBase64(s.getBytes()));
    }

}
