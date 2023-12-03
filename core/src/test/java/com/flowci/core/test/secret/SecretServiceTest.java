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

package com.flowci.core.test.secret;

import com.flowci.core.secret.domain.*;
import com.flowci.core.secret.service.SecretService;
import com.flowci.core.test.MockLoggedInScenario;
import com.flowci.domain.SimpleAuthPair;
import org.assertj.core.util.Strings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yang
 */
public class SecretServiceTest extends MockLoggedInScenario {

    @Autowired
    private SecretService secretService;

    @Test
    void should_create_rsa_secret() {
        Secret rsa = secretService.createRSA("hello.rsa");
        assertNotNull(rsa);
        shouldHasCreatedAtAndCreatedBy(rsa);

        assertEquals(Secret.Category.SSH_RSA, rsa.getCategory());
        assertEquals(sessionManager.getUserEmail(), rsa.getCreatedBy());
        assertNotNull(rsa.getCreatedAt());
        assertNotNull(rsa.getUpdatedAt());

        Secret loaded = secretService.get("hello.rsa");
        assertTrue(loaded instanceof RSASecret);

        RSASecret secret = (RSASecret) loaded;
        assertFalse(Strings.isNullOrEmpty(secret.getPublicKey()));
        assertFalse(Strings.isNullOrEmpty(secret.getPrivateKey()));
        assertNotNull(secret.getMd5Fingerprint());
    }

    @Test
    void should_create_auth_secret() {
        SimpleAuthPair sa = new SimpleAuthPair();
        sa.setUsername("test@flow.ci");
        sa.setPassword("12345");

        Secret auth = secretService.createAuth("hello.auth", sa);
        assertNotNull(auth);
        assertEquals(Secret.Category.AUTH, auth.getCategory());
        assertEquals(sessionManager.getUserEmail(), auth.getCreatedBy());
        assertNotNull(auth.getCreatedAt());
        assertNotNull(auth.getUpdatedAt());

        Secret loaded = secretService.get("hello.auth");
        assertTrue(loaded instanceof AuthSecret);

        AuthSecret keyPair = (AuthSecret) loaded;
        assertFalse(Strings.isNullOrEmpty(keyPair.getUsername()));
        assertFalse(Strings.isNullOrEmpty(keyPair.getPassword()));
    }

    @Test
    void should_list_secret() {
        secretService.createRSA("hello.rsa.1");
        secretService.createRSA("hello.rsa.2");

        secretService.createAuth("hello.auth.1", SimpleAuthPair.of("111", "111"));
        secretService.createAuth("hello.auth.2", SimpleAuthPair.of("111", "111"));

        List<Secret> list = secretService.list();
        assertEquals(4, list.size());

        assertEquals("hello.rsa.1", list.get(0).getName());
        assertEquals("hello.rsa.2", list.get(1).getName());
        assertEquals("hello.auth.1", list.get(2).getName());
        assertEquals("hello.auth.2", list.get(3).getName());

        List<Secret> names = secretService.listName(null);
        assertEquals(4, names.size());

        assertEquals("hello.rsa.1", names.get(0).getName());
        assertEquals("hello.rsa.2", names.get(1).getName());
        assertEquals("hello.auth.1", list.get(2).getName());
        assertEquals("hello.auth.2", list.get(3).getName());
    }

    @Test
    void should_create_android_sign_secret() {
        AndroidSignOption option = new AndroidSignOption();
        option.setKeyStorePassword("12345");
        option.setKeyAlias("helloworld");
        option.setKeyPassword("678910");

        MockMultipartFile ks = new MockMultipartFile("ks", "test.jks", null, "test data".getBytes());

        AndroidSign config = secretService.createAndroidSign("android-debug", ks, option);
        assertEquals("test.jks", config.getKeyStoreFileName());
        assertEquals("12345", config.getKeyStorePassword().getData());

        assertEquals("helloworld", config.getKeyAlias());
        assertEquals("678910", config.getKeyPassword().getData());
    }
}
