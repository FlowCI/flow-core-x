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
package com.flow.platform.api.service;

import com.flow.platform.api.domain.credential.Credential;
import com.flow.platform.api.domain.credential.CredentialDetail;
import com.flow.platform.api.domain.credential.CredentialType;
import com.flow.platform.api.domain.credential.RSAKeyPair;
import com.flow.platform.api.domain.node.Node;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author lhl
 */
public interface CredentialService {

    /**
     * List all credentials or by type
     *
     * @param types set of credential type or null to load all
     */
    List<Credential> list(Collection<CredentialType> types);

    /**
     * Create credential
     */
    Credential createOrUpdate(String name, CredentialDetail detail);

    /**
     * Find credential by node env
     *
     * @return Credential related env
     */
    Map<String, String> findByName(String rsaOrUsernameCredentialName);

    /**
     * find credential by name
     */
    Credential find(String name);

    /**
     * Check credential is existed
     */
    boolean existed(String name);

    /**
     * Delete credential by name
     */
    void delete(String name);

    /**
     * Generate RSA key pair
     */
    RSAKeyPair generateRsaKey();
}
