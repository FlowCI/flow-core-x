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

package com.flowci.core.flow.domain;

import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.SimpleKeyPair;
import com.flowci.util.StringHelper;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * @author yang
 */
@Data
public class FlowGitTest {

    @NotEmpty
    private String gitUrl;

    private String secret;

    private SimpleAuthPair auth;

    private SimpleKeyPair rsa;

    public boolean hasCredential() {
        return StringHelper.hasValue(secret);
    }

    public boolean hasPrivateKey() {
        return rsa != null && StringHelper.hasValue(rsa.getPrivateKey());
    }

    public boolean hasUsernamePassword() {
        return auth != null
            && StringHelper.hasValue(auth.getUsername())
            && StringHelper.hasValue(auth.getPassword());
    }
}
