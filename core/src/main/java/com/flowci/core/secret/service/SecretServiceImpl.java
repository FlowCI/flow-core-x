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

import com.flowci.core.common.helper.CipherHelper;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.secret.dao.SecretDao;
import com.flowci.core.secret.domain.AuthSecret;
import com.flowci.core.secret.domain.RSASecret;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.secret.event.GetSecretEvent;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.SimpleKeyPair;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import com.flowci.util.StringHelper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author yang
 */
@Log4j2
@Service
public class SecretServiceImpl implements SecretService {

    @Autowired
    private SecretDao secretDao;

    @Autowired
    private SessionManager sessionManager;

    @Override
    public List<Secret> list() {
        return secretDao.findAll(Sort.by("createdAt"));
    }

    @Override
    public List<Secret> listName(String category) {
        if (!StringHelper.hasValue(category)) {
            return secretDao.listNameOnly();
        }

        return secretDao.listNameOnly(Secret.Category.valueOf(category));
    }

    @Override
    public Secret get(String name) {
        Optional<Secret> optional = secretDao.findByName(name);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("Secret {0} is not found", name);
    }

    @Override
    public Secret delete(String name) {
        Secret c = get(name);
        secretDao.delete(c);
        return c;
    }

    @Override
    public SimpleKeyPair genRSA() {
        String email = sessionManager.get().getEmail();
        return CipherHelper.RSA.gen(email);
    }

    @Override
    public RSASecret createRSA(String name) {
        String email = sessionManager.get().getEmail();
        SimpleKeyPair pair = CipherHelper.RSA.gen(email);

        RSASecret rsaCredential = new RSASecret();
        rsaCredential.setName(name);
        rsaCredential.setPair(pair);

        return save(rsaCredential);
    }

    @Override
    public RSASecret createRSA(String name, SimpleKeyPair pair) {
        RSASecret rsaCredential = new RSASecret();
        rsaCredential.setName(name);
        rsaCredential.setPair(pair);
        return save(rsaCredential);
    }

    @Override
    public AuthSecret createAuth(String name, SimpleAuthPair pair) {
        AuthSecret auth = new AuthSecret();
        auth.setName(name);
        auth.setPair(pair);
        return save(auth);
    }

    @EventListener
    public void onGetCredentialEvent(GetSecretEvent event) {
        try {
            Secret c = get(event.getName());
            event.setSecret(c);
        } catch (NotFoundException ignore) {
        }
    }

    private <T extends Secret> T save(T credential) {
        try {
            Date now = Date.from(Instant.now());
            credential.setUpdatedAt(now);
            credential.setCreatedAt(now);
            credential.setCreatedBy(sessionManager.getUserId());
            return secretDao.insert(credential);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Secret name {0} is already defined", credential.getName());
        }
    }
}
