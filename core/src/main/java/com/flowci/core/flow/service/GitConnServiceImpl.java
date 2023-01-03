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

import com.flowci.core.common.git.GitClient;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.event.GitTestEvent;
import com.flowci.core.secret.domain.AuthSecret;
import com.flowci.core.secret.domain.RSASecret;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.service.SecretService;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.SimpleKeyPair;
import com.flowci.domain.SimpleSecret;
import com.flowci.exception.ArgumentException;
import com.flowci.util.StringHelper;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * @author yang
 */
@Log4j2
@Service("gitConnService")
@AllArgsConstructor
public class GitConnServiceImpl implements GitConnService {

    private final TaskExecutor appTaskExecutor;

    private final Path tmpDir;

    private final Cache<String, List<String>> gitBranchCache;

    private final SpringEventManager eventManager;

    private final SecretService credentialService;

    @Override
    public void testConn(Flow flow, String url, String secret) {
        Secret c = getSecret(secret);

        if (c != null) {
            if (c.getCategory() != Secret.Category.AUTH && StringHelper.isHttpLink(url)) {
                throw new ArgumentException("Invalid credential for http git url");
            }
        }

        appTaskExecutor.execute(() -> fetchBranchFromGit(flow, url, c));
    }

    @Override
    public void testConn(Flow flow, String url, SimpleKeyPair rsa) {
        if (StringHelper.isHttpLink(url)) {
            throw new ArgumentException("Invalid git url");
        }

        RSASecret c = new RSASecret();
        c.setPair(rsa);

        appTaskExecutor.execute(() -> fetchBranchFromGit(flow, url, c));
    }

    @Override
    public void testConn(Flow flow, String url, SimpleAuthPair auth) {
        if (!StringHelper.isHttpLink(url)) {
            throw new ArgumentException("Invalid git url");
        }

        AuthSecret c = new AuthSecret();
        c.setPair(auth);

        appTaskExecutor.execute(() -> fetchBranchFromGit(flow, url, c));
    }

    @Override
    public List<String> listGitBranch(Flow flow) {
        final String credentialName = flow.getCredentialName();
        return gitBranchCache.get(flow.getId(), (Function<String, List<String>>) flowId ->
                fetchBranchFromGit(flow, flow.getGitUrl(), getSecret(credentialName)));
    }

    //====================================================================
    //        %% Utils
    //====================================================================

    private Secret getSecret(String name) {
        if (!StringHelper.hasValue(name)) {
            return null;
        }

        return credentialService.get(name);
    }

    private List<String> fetchBranchFromGit(Flow flow, String url, Secret credential) {
        if (Strings.isNullOrEmpty(url)) {
            eventManager.publish(new GitTestEvent(this, flow.getId(), "Git url is missing"));
            return Collections.emptyList();
        }

        eventManager.publish(new GitTestEvent(this, flow.getId()));

        SimpleSecret secret = null;
        if (credential != null) {
            secret = credential.toSimpleSecret();
        }
        GitClient client = new GitClient(flow.getGitUrl(), tmpDir, secret);

        try {
            final List<String> branches = client.branches();
            eventManager.publish(new GitTestEvent(this, flow.getId(), branches));
            return branches;
        } catch (Exception e) {
            log.warn(e.getMessage());
            eventManager.publish(new GitTestEvent(this, flow.getId(), e.getMessage()));
            return Collections.emptyList();
        }
    }
}
