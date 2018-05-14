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
package com.flow.platform.api.domain.credential;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;

/**
 * @author lhl
 */
public class RSACredentialDetail extends CredentialDetail {

    @Expose
    @Getter
    @Setter
    private String publicKey;

    @Expose
    @Getter
    @Setter
    private String privateKey;

    public RSACredentialDetail() {
        this.type = CredentialType.RSA;
    }

    public RSACredentialDetail(String publicKey, String privateKey) {
        this();
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public RSACredentialDetail(RSAKeyPair pair) {
        this();
        publicKey = pair.getPublicKey();
        privateKey = pair.getPrivateKey();
    }
}
