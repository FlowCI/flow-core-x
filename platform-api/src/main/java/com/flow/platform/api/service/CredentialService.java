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

import com.flow.platform.api.domain.Credential;
import com.flow.platform.api.domain.CredentialType;
import java.util.List;
import java.util.Map;

/**
 * @author lhl
 */
public interface CredentialService {

    Map<String, String> getKeyMap();

    /**
     * create credential
     */
    Credential create(Credential credential);

    /**
     * find credential by name
     */
    Credential find(String name);

    /**
     * update credential
     */

    Credential update(Credential credential);

    boolean delete(String name);

    List<Credential> listCredentials();

    List<Credential> listTypes(String credentialType);

}
