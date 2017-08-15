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
import com.flow.platform.api.domain.Credential;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author lhl
 */

@Service(value = "credentialService")
public class CredentialServiceImpl implements CredentialService {

    @Autowired
    private CredentialDao credentialDao;

    @Override
    public Credential create(Credential credential) {
        credentialDao.save(credential);
        return credential;
    }

    @Override
    public Credential find(String name) {
        Credential credential = credentialDao.get(name);
        return credential;
    }

    @Override
    public Credential update(Credential credential) {
        credentialDao.update(credential);
        return credential;

    }

    @Override
    public boolean delete(String name) {
        Credential credential = find(name);
        credentialDao.delete(credential);
        return true;
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
            ByteArrayOutputStream baos = new ByteArrayOutputStream();//向OutPutStream中写入
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

}