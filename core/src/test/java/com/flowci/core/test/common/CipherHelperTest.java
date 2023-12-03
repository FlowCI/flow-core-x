/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.test.common;

import com.flowci.common.helper.StringHelper;
import com.flowci.core.common.helper.CipherHelper;
import com.google.common.base.Strings;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import static com.flowci.core.common.helper.CipherHelper.RSA.fingerprintMd5;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class CipherHelperTest {

    private final String source = "!@#!@#!@1fsd";

    @Test
    public void should_encrypt_decrypt_by_aes() {
        final String secret = "ssdkF$HUy2A#D%kd";

        String encrypted = CipherHelper.AES.encrypt(source, secret);
        assertFalse(Strings.isNullOrEmpty(encrypted));

        String decrypted = CipherHelper.AES.decrypt(encrypted, secret);
        assertFalse(Strings.isNullOrEmpty(decrypted));

        assertEquals(source, decrypted);
    }

    @Test
    void should_create_public_key_fingerprint() throws IOException, NoSuchAlgorithmException {
        InputStream in = CipherHelper.class.getClassLoader().getResourceAsStream("pk_fingerprint");
        String publicKey = StringHelper.toString(in);
        assertEquals("09:e6:ce:d3:ba:a3:ee:75:9e:96:7b:55:12:85:c6:4e", fingerprintMd5(publicKey));
    }
}
