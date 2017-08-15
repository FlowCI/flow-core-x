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
import com.flow.platform.api.dao.CredentialDao;
import com.flow.platform.api.domain.Credential;
import com.flow.platform.api.domain.CredentialType;
import com.flow.platform.api.service.CredentialService;
import com.flow.platform.api.test.TestBase;
import java.time.ZonedDateTime;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class CredentialDaoTest extends TestBase {
    @Autowired
    private CredentialDao credentialDao;

    @Autowired
    private CredentialService credentialService;

    @Test
    public void should_save_and_get_success(){
        Credential credential = new Credential();
        String publicKey = credentialService.getKeyMap().get("publicKey");
        credential.setPublicKey(publicKey);
        credential.setCredentialType(CredentialType.SSHKEY);
        credential.setName("test");
        credential.setCreatedAt(ZonedDateTime.now());
        credential.setUpdatedAt(ZonedDateTime.now());
        credentialDao.save(credential);
        Assert.assertEquals(2, credentialService.getKeyMap().size());
        Assert.assertEquals("test",credential.getName());
    }

    @Test
    public void should_update_credential_success(){
        Credential credential = new Credential();
        String publicKey = credentialService.getKeyMap().get("publicKey");
        credential.setPublicKey(publicKey);
        credential.setCredentialType(CredentialType.SSHKEY);
        credential.setName("test");
        credential.setCreatedAt(ZonedDateTime.now());
        credential.setUpdatedAt(ZonedDateTime.now());
        credentialDao.save(credential);

        Credential credential1 = credentialDao.get("test");
        credential1.setCredentialType(CredentialType.USERNAME);
        credentialDao.update(credential1);
        Assert.assertEquals(CredentialType.USERNAME, credential1.getCredentialType());
    }


    @Test
    public void should_delete_success(){
        Credential credential = new Credential();
        String publicKey = credentialService.getKeyMap().get("publicKey");
        credential.setPublicKey(publicKey);
        credential.setCredentialType(CredentialType.SSHKEY);
        credential.setName("test");
        credential.setCreatedAt(ZonedDateTime.now());
        credential.setUpdatedAt(ZonedDateTime.now());
        credentialDao.save(credential);
        credentialDao.delete(credential);
        Assert.assertEquals(0, credentialDao.list().size());
    }
}
