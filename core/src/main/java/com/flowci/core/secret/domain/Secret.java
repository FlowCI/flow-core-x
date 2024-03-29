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

package com.flowci.core.secret.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.core.common.domain.Mongoable;
import com.flowci.common.domain.SimpleSecret;
import com.flowci.store.Pathable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author yang
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Document(collection = "secret")
public class Secret extends Mongoable {

    public enum Category {

        AUTH,

        SSH_RSA,

//        SSH_DSS,
//
//        SSH_ED25519,

        TOKEN,

        ANDROID_SIGN,

        KUBE_CONFIG
    }

    @Indexed(name = "index_secret_name", unique = true)
    private String name;

    private Category category;

    public SimpleSecret toSimpleSecret() {
        return null;
    }

    @JsonIgnore
    @Transient
    public Pathable[] getPath() {
        return new Pathable[]{
                () -> "secret",
                this::getName,
        };
    }
}
