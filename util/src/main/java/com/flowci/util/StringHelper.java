/*
 * Copyright 2018 flow.ci
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

package com.flowci.util;

import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * @author yang
 */
public abstract class StringHelper {

    public static final String EMPTY = "";

    private static final SecureRandom random = new SecureRandom();
    
    private static final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public static boolean hasValue(String value) {
        return !Strings.isNullOrEmpty(value);
    }

    public static boolean isHttpLink(String value) {
        if (!hasValue(value)) {
            return false;
        }

        return value.startsWith("http://") || value.startsWith("https://");
    }

    public static String toString(InputStream is) throws IOException {
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            int length;
            byte[] buffer = new byte[1024];
            while ((length = is.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8.name());
        }
    }

    public static InputStream toInputStream(String val) {
        return new ByteArrayInputStream(val.getBytes());
    }

    public static String toHex(String str) {
        byte[] bytes = str.getBytes();
        char[] chars = new char[2 * bytes.length];

        for (int i = 0; i < bytes.length; ++i) {
            chars[2 * i] = HEX_CHARS[(bytes[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[bytes[i] & 0x0F];
        }

        return new String(chars);
    }

    public static String fromHex(String hex) {
        byte[] txtInByte = new byte[hex.length() / 2];
        int j = 0;
        for (int i = 0; i < hex.length(); i += 2) {
            txtInByte[j++] = Byte.parseByte(hex.substring(i, i + 2), 16);
        }
        return new String(txtInByte);
    }

    public static String toBase64(String str) {
        return Base64.getEncoder().encodeToString(str.getBytes());
    }

    public static String fromBase64(String base64) {
        byte[] decode = Base64.getDecoder().decode(base64.getBytes());
        return new String(decode);
    }

    public static String randomString(int length) {
        byte[] buffer = new byte[length];
        random.nextBytes(buffer);
        return encoder.encodeToString(buffer).toLowerCase();
    }
}
