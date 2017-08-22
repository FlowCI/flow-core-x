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
package com.flow.platform.api.test.dao;

import com.flow.platform.api.dao.CredentialStorageDao;
import com.flow.platform.api.domain.Credential;
import com.flow.platform.api.domain.CredentialStorage;
import com.flow.platform.api.domain.CredentialType;
import com.flow.platform.api.domain.SSHKey;
import com.flow.platform.api.service.CredentialService;
import com.flow.platform.api.test.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class CredentialStorageDaoTest extends TestBase {

    @Autowired
    private CredentialStorageDao credentialStorageDao;

    @Autowired
    private CredentialService credentialService;

    @Test
    public void should_save_and_get_success() {
        CredentialStorage credentialStorage = new CredentialStorage();
        SSHKey sshKey = new SSHKey();
        String publicKey = credentialService.getKeyMap().get("publicKey");
        sshKey.setPublicKey(publicKey);
        sshKey.setCredentialType(CredentialType.RSAkEYS);
        sshKey.setName("test");
        credentialStorage.setContent(sshKey);
        credentialStorageDao.save(credentialStorage);
        Assert.assertEquals(2, credentialService.getKeyMap().size());
        Assert.assertEquals("test", credentialStorage.getContent().getName());
    }

    @Test
    public void should_update_credential_success() {
        CredentialStorage credentialStorage = new CredentialStorage();
        SSHKey sshKey = new SSHKey();
        String publicKey = credentialService.getKeyMap().get("publicKey");
        sshKey.setPublicKey(publicKey);
        sshKey.setCredentialType(CredentialType.RSAkEYS);
        sshKey.setName("test");
        credentialStorage.setContent(sshKey);
        credentialStorageDao.save(credentialStorage);

        Credential credential1 = credentialStorage.getContent();
        credential1.setCredentialType(CredentialType.USERNAME);
        credentialStorage.setContent(credential1);
        credentialStorageDao.update(credentialStorage);
        Assert.assertEquals(CredentialType.USERNAME, credentialStorage.getContent().getCredentialType());
    }


    @Test
    public void should_delete_success() {
        CredentialStorage credentialStorage = new CredentialStorage();
        SSHKey sshKey = new SSHKey();
        String publicKey = credentialService.getKeyMap().get("publicKey");
        sshKey.setPublicKey(publicKey);
        sshKey.setCredentialType(CredentialType.RSAkEYS);
        sshKey.setName("test");
        credentialStorageDao.save(credentialStorage);
        credentialStorageDao.delete(credentialStorage);
        Assert.assertEquals(0, credentialStorageDao.list().size());
    }
}
