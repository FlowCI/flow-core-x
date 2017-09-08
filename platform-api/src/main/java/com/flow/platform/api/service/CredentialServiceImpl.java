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

import com.flow.platform.api.dao.CredentialDao;
import com.flow.platform.api.domain.credential.AndroidCredentialDetail;
import com.flow.platform.api.domain.credential.Credential;
import com.flow.platform.api.domain.credential.CredentialDetail;
import com.flow.platform.api.domain.credential.CredentialType;
import com.flow.platform.api.domain.credential.IosCredentialDetail;
import com.flow.platform.api.domain.credential.RSACredentialDetail;
import com.flow.platform.api.domain.credential.RSAKeyPair;
import com.flow.platform.api.domain.credential.UsernameCredentialDetail;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.util.CollectionUtil;
import com.flow.platform.util.StringUtil;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lhl
 */

@Service
@Transactional
public class CredentialServiceImpl implements CredentialService {

    @Autowired
    private CredentialDao credentialDao;

    private final Map<CredentialType, DetailHandler> handlerMapping = new HashMap<>();

    @PostConstruct
    public void init() {
        handlerMapping.put(CredentialType.RSA, new RSADetailHandler());
        handlerMapping.put(CredentialType.USERNAME, new UsernameDetailHandler());
        handlerMapping.put(CredentialType.ANDROID, new AndroidDetailHandler());
        handlerMapping.put(CredentialType.IOS, new IosDetailHandler());
    }

    @Override
    public List<Credential> list(Collection<CredentialType> types) {
        if (CollectionUtil.isNullOrEmpty(types)) {
            return credentialDao.list();
        }

        return credentialDao.listByType(types);
    }

    @Override
    public Credential createOrUpdate(String name, CredentialDetail detail) {
        Credential credential = credentialDao.get(name);

        if (credential == null) {
            credential = new Credential(name);
        }

        // createOrUpdate xxCredentialDetailHandler instance by name
        handlerMapping.get(detail.getType()).handle(detail);

        credential.setType(detail.getType());
        credential.setDetail(detail);
        credentialDao.saveOrUpdate(credential);

        return credentialDao.get(name);
    }

    @Override
    public Credential find(String name) {
        Credential credential = credentialDao.get(name);

        if (credential == null) {
            throw new IllegalParameterException("Credential '" + name + "' doesn't existed");
        }

        return credential;
    }

    @Override
    public void delete(String name) {
        Credential credential = find(name);
        credentialDao.delete(credential);
    }

    @Override
    public RSAKeyPair generateRsaKey() {
        String comment = "FLOWCI";
        int type = KeyPair.RSA;
        JSch jsch = new JSch();

        try {
            KeyPair kpair = KeyPair.genKeyPair(jsch, type);
            RSAKeyPair pair = new RSAKeyPair();

            // private key
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                kpair.writePrivateKey(baos);
                pair.setPrivateKey(baos.toString());
            }

            // public key
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                kpair.writePublicKey(baos, comment);
                pair.setPublicKey(baos.toString());
            }

            kpair.dispose();
            return pair;

        } catch (JSchException | IOException e) {
            return null;
        }
    }

    private interface DetailHandler<T extends CredentialDetail> {

        void handle(T detail);
    }

    private class RSADetailHandler implements DetailHandler<RSACredentialDetail> {

        @Override
        public void handle(RSACredentialDetail detail) {
            if (StringUtil.isNullOrEmptyForItems(detail.getPrivateKey(), detail.getPublicKey())) {
                RSAKeyPair pair = generateRsaKey();
                detail.setPublicKey(pair.getPublicKey());
                detail.setPrivateKey(pair.getPrivateKey());
            }
        }
    }

    private class UsernameDetailHandler implements DetailHandler<UsernameCredentialDetail> {

        @Override
        public void handle(UsernameCredentialDetail detail) {

        }
    }

    private class AndroidDetailHandler implements DetailHandler<AndroidCredentialDetail> {

        @Override
        public void handle(AndroidCredentialDetail detail) {

        }
    }

    private class IosDetailHandler implements DetailHandler<IosCredentialDetail> {

        @Override
        public void handle(IosCredentialDetail detail) {

        }
    }
}