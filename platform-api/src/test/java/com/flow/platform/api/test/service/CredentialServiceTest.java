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

import com.flow.platform.api.dao.CredentialStorageDao;
import com.flow.platform.api.domain.CredentialStorage;
import com.flow.platform.api.domain.CredentialType;
import com.flow.platform.api.domain.CredentialUserName;
import com.flow.platform.api.service.CredentialService;
import com.flow.platform.api.test.TestBase;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

/**
 * @author lhl
 */
public class CredentialServiceTest extends TestBase {

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private CredentialStorageDao credentialStorageDao;

    @Test
    public void should_create_credential() {
        CredentialUserName credentialUserName = new CredentialUserName();
        credentialUserName.setName("test");
        credentialUserName.setUserName("name1");
        credentialUserName.setPassword("password");
        credentialUserName.setCredentialType(CredentialType.USERNAME);
        credentialService.create(credentialUserName);
        Assert.assertEquals(credentialUserName.getName(), credentialService.find("test").getName());
    }

    @Test
    public void should_generate_ssh_key() {
        Map<String, String> keys = credentialService.getKeyMap();
        Assert.assertEquals(2, keys.size());
    }

    @Test
    public void should_find_credential() {
        CredentialUserName credential = new CredentialUserName();
        CredentialStorage credentialStorage = new CredentialStorage();
        credential.setName("test");
        credential.setUserName("name1");
        credential.setPassword("password");
        credential.setCredentialType(CredentialType.USERNAME);
        credentialStorage.setContent(credential);
        credentialStorageDao.save(credentialStorage);
        Assert.assertEquals(CredentialType.USERNAME, credentialService.find("test").getCredentialType());
    }

    @Test
    public void should_update_credential() {
        CredentialStorage credentialStorage = new CredentialStorage();
        CredentialUserName credentialUserName = new CredentialUserName();
        credentialUserName.setName("test");
        credentialUserName.setUserName("name1");
        credentialUserName.setPassword("password");
        credentialUserName.setCredentialType(CredentialType.USERNAME);
        credentialStorage.setContent(credentialUserName);
        credentialStorageDao.save(credentialStorage);
        CredentialUserName credential1 = (CredentialUserName) credentialService.find("test");
        credential1.setUserName("name2");
        credentialService.update(credential1);
        Assert.assertEquals("name2", ((CredentialUserName) credentialService.find("test")).getUserName());
    }

    @Test
    public void should_delete_credential() {
        CredentialStorage credentialStorage = new CredentialStorage();
        CredentialUserName credentialUserName = new CredentialUserName();
        credentialUserName.setName("test");
        credentialUserName.setUserName("name1");
        credentialUserName.setPassword("password");
        credentialUserName.setCredentialType(CredentialType.USERNAME);
        credentialStorage.setContent(credentialUserName);
        credentialStorageDao.save(credentialStorage);
        credentialService.delete("test");
        Assert.assertEquals(0, credentialService.listCredentials().size());
    }

    @Test
    public void should_list_credentials(){
        CredentialStorage credentialStorage = new CredentialStorage();
        CredentialUserName credentialUserName = new CredentialUserName();
        credentialUserName.setName("test");
        credentialUserName.setUserName("name1");
        credentialUserName.setPassword("password");
        credentialUserName.setCredentialType(CredentialType.USERNAME);
        credentialStorage.setContent(credentialUserName);
        credentialStorageDao.save(credentialStorage);

        Assert.assertEquals(1, credentialService.listCredentials().size());
    }

    @Test
    public void should_list_by_types(){
        CredentialStorage credentialStorage = new CredentialStorage();
        CredentialUserName credentialUserName = new CredentialUserName();
        credentialUserName.setName("test");
        credentialUserName.setUserName("name1");
        credentialUserName.setPassword("password");
        credentialUserName.setCredentialType(CredentialType.USERNAME);
        credentialStorage.setContent(credentialUserName);
        credentialStorageDao.save(credentialStorage);

        Assert.assertEquals(1, credentialService.listTypes("USERNAME").size());
    }

    @Test
    public void should_limit_file_size(){
        long allowSize = credentialService.getAllowSize();
        byte[] mockData = "test".getBytes();
        String originalFilename = "1.out.zip";
        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", originalFilename, "", mockData);
        Assert.assertEquals(true, allowSize > mockMultipartFile.getSize());
    }

    @Test
    public void should_limit_file_suffix(){
        String allowSuffix = credentialService.allowSuffix();
        byte[] mockData = "test".getBytes();
        String originalFilename = "1.zip";
        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", originalFilename, "", mockData);
        String suffix = mockMultipartFile.getOriginalFilename().substring(mockMultipartFile.getOriginalFilename().lastIndexOf(".") + 1);
        int length = allowSuffix.indexOf(suffix);
        Assert.assertEquals(true, length == -1);
    }

}
