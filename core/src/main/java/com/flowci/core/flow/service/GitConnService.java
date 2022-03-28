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

package com.flowci.core.flow.service;

import com.flowci.core.flow.domain.Flow;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.SimpleKeyPair;

import java.util.List;

/**
 * @author yang
 */
public interface GitConnService {

    /**
     * Test git connection for http or ssh with credential
     *
     * @param secret nullable
     */
    void testConn(Flow flow, String url, String secret);

    /**
     * Test git connection for ssh url with private key
     */
    void testConn(Flow flow, String url, SimpleKeyPair rsa);

    /**
     * Test git connection for http url with username and password
     */
    void testConn(Flow flow, String url, SimpleAuthPair auth);

    /**
     * List remote branches
     *
     * @return list of branches or empty list if git config not defined
     */
    List<String> listGitBranch(Flow flow);
}
