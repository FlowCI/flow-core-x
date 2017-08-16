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
package com.flow.platform.api.test.service;

import com.flow.platform.api.dao.CredentialDao;
import com.flow.platform.api.domain.Credential;
import com.flow.platform.api.domain.CredentialType;
import com.flow.platform.api.service.CredentialService;
import com.flow.platform.api.test.TestBase;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class CredentialServiceTest extends TestBase {

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private CredentialDao credentialDao;

    @Test
    public void should_create_credential() {
        Credential credential = new Credential();
        credential.setName("test");
        credential.setUserName("name1");
        credential.setPassword("password");
        credential.setCredentialType(CredentialType.USERNAME);
        credentialService.create(credential);
        Assert.assertEquals(credential.getUserName(), credentialService.find("test").getUserName());
    }

    @Test
    public void should_generate_ssh_key() {
        Map<String, String> keys = credentialService.getKeyMap();
        Assert.assertEquals(2, keys.size());
    }

    @Test
    public void should_find_credential() {
        Credential credential = new Credential();
        credential.setName("test");
        credential.setUserName("name1");
        credential.setPassword("password");
        credential.setCredentialType(CredentialType.USERNAME);
        credentialDao.save(credential);
        Assert.assertEquals("name1", credentialService.find("test").getUserName());
    }

    @Test
    public void should_update_credential() {
        Credential credential = new Credential();
        credential.setName("test");
        credential.setUserName("name1");
        credential.setPassword("password");
        credential.setCredentialType(CredentialType.USERNAME);
        credentialDao.save(credential);
        Credential credential1 = credentialService.find("test");
        credential1.setUserName("name2");
        credentialService.update(credential1);
        Assert.assertEquals("name2", credential1.getUserName());
    }

    @Test
    public void should_delete_credential() {
        Credential credential = new Credential();
        credential.setName("test");
        credential.setUserName("name1");
        credential.setPassword("password");
        credential.setCredentialType(CredentialType.USERNAME);
        credentialDao.save(credential);
        credentialService.delete("test");
        Assert.assertEquals(0, credentialService.listCredentials().size());
    }

    @Test
    public void should_list_credentials(){
        Credential credential = new Credential();
        credential.setName("test");
        credential.setUserName("name1");
        credential.setPassword("password");
        credential.setCredentialType(CredentialType.USERNAME);
        credentialDao.save(credential);

        Assert.assertEquals(1, credentialService.listCredentials().size());
    }

}
