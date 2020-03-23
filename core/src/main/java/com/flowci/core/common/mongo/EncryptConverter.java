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

package com.flowci.core.common.mongo;

import com.flowci.core.common.helper.CipherHelper;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.SimpleKeyPair;
import lombok.Getter;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;

@Getter
public class EncryptConverter {

    private static final String FieldPublicKey = "publicKey";

    private static final String FieldPrivateKey = "privateKey";

    private static final String FieldUsername = "username";

    private static final String FieldPassword = "password";

    private final String appSecret;

    public EncryptConverter(String appSecret) {
        this.appSecret = appSecret;
    }

    public class SimpleKeyPairReader implements Converter<Document, SimpleKeyPair> {

        @Override
        public SimpleKeyPair convert(Document source) {
            String encryptedPublicKey = source.getString(FieldPublicKey);
            String encryptedPrivateKey = source.getString(FieldPrivateKey);

            return SimpleKeyPair.of(
                CipherHelper.AES.decrypt(encryptedPublicKey, appSecret),
                CipherHelper.AES.decrypt(encryptedPrivateKey, appSecret)
            );
        }
    }

    public class SimpleKeyPairWriter implements Converter<SimpleKeyPair, Document> {

        @Override
        public Document convert(SimpleKeyPair pair) {
            Document document = new Document();
            document.put(FieldPublicKey, CipherHelper.AES.encrypt(pair.getPublicKey(), appSecret));
            document.put(FieldPrivateKey, CipherHelper.AES.encrypt(pair.getPrivateKey(), appSecret));
            return document;
        }
    }

    public class SimpleAuthPairReader implements Converter<Document, SimpleAuthPair> {

        @Override
        public SimpleAuthPair convert(Document source) {
            String encryptedUsername = source.getString(FieldUsername);
            String encryptedPassword = source.getString(FieldPassword);

            return SimpleAuthPair.of(
                CipherHelper.AES.decrypt(encryptedUsername, appSecret),
                CipherHelper.AES.decrypt(encryptedPassword, appSecret)
            );
        }
    }

    public class SimpleAuthPairWriter implements Converter<SimpleAuthPair, Document> {

        @Override
        public Document convert(SimpleAuthPair pair) {
            Document document = new Document();
            document.put(FieldUsername, CipherHelper.AES.encrypt(pair.getUsername(), appSecret));
            document.put(FieldPassword, CipherHelper.AES.encrypt(pair.getPassword(), appSecret));
            return document;
        }
    }
}
