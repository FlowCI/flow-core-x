/*
 * Copyright 2019 flow.ci
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
import com.flowci.common.domain.SimpleAuthPair;
import com.flowci.common.domain.SimpleSecret;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Username and password credential
 *
 * @author yang
 */
@Getter
@Setter
@Document(collection = "secret")
public class AuthSecret extends Secret {

    private SimpleAuthPair pair;

    public AuthSecret() {
        this.pair = new SimpleAuthPair();
        this.setCategory(Category.AUTH);
    }

    @JsonIgnore
    public String getUsername() {
        return pair.getUsername();
    }

    @JsonIgnore
    public String getPassword() {
        return pair.getPassword();
    }

    @Override
    public SimpleSecret toSimpleSecret() {
        return pair;
    }
}
