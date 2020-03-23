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

package com.flowci.core.credential.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.domain.SimpleKeyPair;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author yang
 */
@Getter
@Setter
@Document(collection = "credential")
public final class RSACredential extends Credential {

    private SimpleKeyPair pair;

    public RSACredential() {
        this.pair = new SimpleKeyPair();
        this.setCategory(Category.SSH_RSA);
    }

    @JsonIgnore
    public String getPrivateKey() {
        return pair.getPrivateKey();
    }

    @JsonIgnore
    public String getPublicKey() {
        return pair.getPublicKey();
    }

    public void setPrivateKey(String pk) {
        this.pair.setPrivateKey(pk);
    }

    public void setPublicKey(String pk) {
        this.pair.setPublicKey(pk);
    }
}
