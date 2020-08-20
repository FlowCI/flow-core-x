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

import com.flowci.core.common.domain.Mongoable;
import com.flowci.core.common.helper.CipherHelper;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.secret.dao.SecretDao;
import com.flowci.core.secret.domain.*;
import com.flowci.core.secret.event.CreateAuthEvent;
import com.flowci.core.secret.event.CreateRsaEvent;
import com.flowci.core.secret.event.GetSecretEvent;
import com.flowci.docker.K8sManager;
import com.flowci.domain.SecretField;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.SimpleKeyPair;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.store.FileManager;
import com.flowci.util.StringHelper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
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

    @Autowired
    private FileManager fileManager;

    @Override
    public List<Secret> list() {
        return secretDao.findAll(Mongoable.SortByCreatedAtASC);
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

        if (c instanceof AndroidSign) {
            try {
                AndroidSign sign = (AndroidSign) c;
                fileManager.remove(sign.getKeyStoreFileName(), sign.getPath());
                log.info("Secret file {} been deleted", sign.getKeyStoreFileName());
            } catch (IOException e) {
                log.warn(e.getMessage());
            }
        }

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
        return createRSA(name, pair);
    }

    @Override
    public RSASecret createRSA(String name, SimpleKeyPair pair) {
        try {
            RSASecret secret = new RSASecret();
            secret.setName(name);
            secret.setPair(pair);
            secret.setMd5Fingerprint(CipherHelper.RSA.fingerprintMd5(pair.getPublicKey()));
            return save(secret);
        } catch (NoSuchAlgorithmException e) {
            throw new StatusException("failed to generate fingerprint");
        }
    }

    @Override
    public AuthSecret createAuth(String name, SimpleAuthPair pair) {
        AuthSecret auth = new AuthSecret();
        auth.setName(name);
        auth.setPair(pair);
        return save(auth);
    }

    @Override
    public TokenSecret createToken(String name, String token) {
        TokenSecret t = new TokenSecret();
        t.setName(name);
        t.setTokenData(token);
        return save(t);
    }

    @Override
    public AndroidSign createAndroidSign(String name, MultipartFile keyStore, AndroidSignOption option) {
        try {
            AndroidSign secret = new AndroidSign();
            secret.setName(name);
            secret.setKeyStoreFileName(keyStore.getOriginalFilename());
            secret.setKeyStorePassword(SecretField.of(option.getKeyStorePassword()));
            secret.setKeyAlias(option.getKeyAlias());
            secret.setKeyPassword(SecretField.of(option.getKeyPassword()));

            fileManager.save(keyStore.getOriginalFilename(), keyStore.getInputStream(), secret.getPath());
            return save(secret);
        } catch (IOException e) {
            throw new StatusException(e.getMessage());
        }
    }

    @Override
    public KubeConfigSecret createKubeConfig(String name, String content) {
        try {
            K8sManager.parse(content);
            KubeConfigSecret secret = new KubeConfigSecret();
            secret.setName(name);
            secret.setContent(SecretField.of(content));
            return save(secret);
        } catch (Exception e) {
            throw new StatusException(e.getMessage());
        }
    }

    @EventListener
    public void onGetCredentialEvent(GetSecretEvent event) {
        try {
            Secret c = get(event.getName());
            event.setFetched(c);
        } catch (NotFoundException e) {
            event.setError(e);
        }
    }

    @EventListener
    public void onCreateRsaEvent(CreateRsaEvent event) {
        try {
            RSASecret secret = createRSA(event.getName(), event.getPair());
            event.setSecret(secret);
        } catch (DuplicateException e) {
            event.setErr(e);
        }
    }

    @EventListener
    public void onCreateAuthEvent(CreateAuthEvent event) {
        try {
            AuthSecret secret = createAuth(event.getName(), event.getPair());
            event.setSecret(secret);
        } catch (DuplicateException e) {
            event.setErr(e);
        }
    }

    private <T extends Secret> T save(T credential) {
        try {
            return secretDao.insert(credential);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Secret name {0} is already defined", credential.getName());
        }
    }
}
