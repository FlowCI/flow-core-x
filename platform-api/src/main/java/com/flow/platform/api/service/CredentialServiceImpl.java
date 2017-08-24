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

import static com.flow.platform.api.config.AppConfig.ALLOW_SIZE;
import static com.flow.platform.api.config.AppConfig.ALLOW_SUFFIX;

import com.flow.platform.api.dao.CredentialStorageDao;
import com.flow.platform.api.domain.Credential;
import com.flow.platform.api.domain.CredentialStorage;
import com.flow.platform.api.domain.CredentialType;
import com.flow.platform.core.exception.IllegalParameterException;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import java.io.ByteArrayOutputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author lhl
 */

@Service(value = "credentialService")
public class CredentialServiceImpl implements CredentialService {

    @Autowired
    private CredentialStorageDao credentialStorageDao;

    @Override
    public Credential create(Credential credential) {
        CredentialStorage credentialStorage = new CredentialStorage(credential, ZonedDateTime.now(),
            ZonedDateTime.now());
        if(findCredentialByName(credential.getName()) != null){
            throw new IllegalParameterException(String.format("name is already present"));
        } else {
            credentialStorageDao.save(credentialStorage);
            return credential;
        }

    }

    @Override
    public Credential find(String name) {
        return findCredentialByName(name).getContent();
    }

    @Override
    public Credential update(Credential credential) {
        CredentialStorage credentialStorage = findCredentialByName(credential.getName());
        credentialStorage.setContent(credential);
        credentialStorageDao.update(credentialStorage);
        return credential;
    }

    @Override
    public void delete(String name) {
        CredentialStorage credentialStorage = findCredentialByName(name);
        credentialStorageDao.delete(credentialStorage);
    }

    @Override
    public List<Credential> listCredentials() {
        return listCertificate();
    }

    @Override
    public List<Credential> listTypes(String credentialType) {
        List<Credential> list = new ArrayList<>();
        CredentialType credentialType1 = CredentialType.valueOf(credentialType);
        List<Credential> list_certificate = listCertificate();
        for (Credential credential : list_certificate) {
            if (credential.getCredentialType() == credentialType1) {
                list.add(credential);
            }
        }
        return list;
    }


    @Override
    public Map<String, String> getKeyMap() {
        String comment = "FLOWCI";
        Map<String, String> keys = new HashMap<>();
        int type = KeyPair.RSA;
        JSch jsch = new JSch();
        try {
            KeyPair kpair = KeyPair.genKeyPair(jsch, type);
            //私钥
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            kpair.writePrivateKey(baos);
            String privateKeyString = baos.toString();
            //公钥
            baos = new ByteArrayOutputStream();
            kpair.writePublicKey(baos, comment);
            String publicKeyString = baos.toString();
            kpair.dispose();
            // 得到公钥字符串
            keys.put("publicKey", publicKeyString);
            // 得到私钥字符串
            keys.put("privateKey", privateKeyString);
        } catch (Exception e) {
            return null;
        }
        return keys;
    }

    public long getAllowSize(){
        return ALLOW_SIZE;
    }

    public String allowSuffix(){
        return ALLOW_SUFFIX;
    }

    private CredentialStorage findCredentialByName(String name) {
        for (CredentialStorage credentialStorage : credentialStorageDao.list()) {
            if (credentialStorage.getContent().getName().equals(name)) {
                return credentialStorage;
            }
        }
        return null;
    }

    private List<Credential> listCertificate() {
        List<Credential> list = new ArrayList<>();
        for (CredentialStorage credentialStorage : credentialStorageDao.list()) {
            list.add(credentialStorage.getContent());
        }
        return list;
    }

}