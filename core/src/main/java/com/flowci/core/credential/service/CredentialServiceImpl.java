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

package com.flowci.core.credential.service;

import com.flowci.core.common.helper.CipherHelper;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.credential.dao.CredentialDao;
import com.flowci.core.credential.domain.AuthCredential;
import com.flowci.core.credential.domain.Credential;
import com.flowci.core.credential.domain.Credential.Category;
import com.flowci.core.credential.domain.RSACredential;
import com.flowci.core.credential.event.GetCredentialEvent;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.SimpleKeyPair;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import com.flowci.util.StringHelper;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class CredentialServiceImpl implements CredentialService {

    @Autowired
    private CredentialDao credentialDao;

    @Autowired
    private SessionManager sessionManager;

    @Override
    public List<Credential> list() {
        return credentialDao.findAll(Sort.by("createdAt"));
    }

    @Override
    public List<Credential> listName(String category) {
        if (!StringHelper.hasValue(category)) {
            return credentialDao.listNameOnly();
        }

        return credentialDao.listNameOnly(Category.valueOf(category));
    }

    @Override
    public Credential get(String name) {
        Optional<Credential> optional = credentialDao.findByName(name);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("Credential {0} is not found", name);
    }

    @Override
    public Credential delete(String name) {
        Credential c = get(name);
        credentialDao.delete(c);
        return c;
    }

    @Override
    public SimpleKeyPair genRSA() {
        String email = sessionManager.get().getEmail();
        return CipherHelper.RSA.gen(email);
    }

    @Override
    public RSACredential createRSA(String name) {
        String email = sessionManager.get().getEmail();
        SimpleKeyPair pair = CipherHelper.RSA.gen(email);

        RSACredential rsaCredential = new RSACredential();
        rsaCredential.setName(name);
        rsaCredential.setPair(pair);

        return save(rsaCredential);
    }

    @Override
    public RSACredential createRSA(String name, SimpleKeyPair pair) {
        RSACredential rsaCredential = new RSACredential();
        rsaCredential.setName(name);
        rsaCredential.setPair(pair);
        return save(rsaCredential);
    }

    @Override
    public AuthCredential createAuth(String name, SimpleAuthPair pair) {
        AuthCredential auth = new AuthCredential();
        auth.setName(name);
        auth.setPair(pair);
        return save(auth);
    }

    @EventListener
    public void onGetCredentialEvent(GetCredentialEvent event) {
        try {
            Credential c = get(event.getName());
            event.setCredential(c);
        } catch (NotFoundException ignore) {
        }
    }

    private <T extends Credential> T save(T credential) {
        try {
            Date now = Date.from(Instant.now());
            credential.setUpdatedAt(now);
            credential.setCreatedAt(now);
            credential.setCreatedBy(sessionManager.getUserId());
            return credentialDao.insert(credential);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Credential name {0} is already defined", credential.getName());
        }
    }
}
