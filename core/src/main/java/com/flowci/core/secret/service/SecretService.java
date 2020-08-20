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

package com.flowci.core.secret.service;

import com.flowci.core.secret.domain.*;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.SimpleKeyPair;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author yang
 */
public interface SecretService {

    /**
     * List credential for current user
     */
    List<Secret> list();

    /**
     * List credential name only for current user
     */
    List<Secret> listName(String category);

    /**
     * Get credential for current user
     */
    Secret get(String name);

    /**
     * Delete credential by name
     */
    Secret delete(String name);

    /**
     * Generate RSA key pair
     */
    SimpleKeyPair genRSA();

    /**
     * Create rsa key pair which is generated automatically
     */
    RSASecret createRSA(String name);

    /**
     * Create rsa key pair which is given from user
     */
    RSASecret createRSA(String name, SimpleKeyPair pair);

    /**
     * Create auth username, password pair
     */
    AuthSecret createAuth(String name, SimpleAuthPair pair);

    /**
     * Create token secret
     */
    TokenSecret createToken(String name, String token);

    /**
     * Create Android Sign Secret
     */
    AndroidSign createAndroidSign(String name, MultipartFile keyStore, AndroidSignOption option);

    /**
     * Create kube config secret
     */
    KubeConfigSecret createKubeConfig(String name, String content);
}
