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

import com.flow.platform.api.domain.credential.Credential;
import com.flow.platform.api.domain.credential.CredentialType;
import com.flow.platform.api.domain.credential.RSACredentialDetail;
import com.flow.platform.api.domain.credential.UsernameCredentialDetail;
import com.flow.platform.api.test.TestBase;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author lhl
 */
public class CredentialDaoTest extends TestBase {

    private Credential rsaCredential;

    private Credential usernameCredential;

    @Before
    public void before() {
        RSACredentialDetail sshKey = new RSACredentialDetail("public key", "private key");
        rsaCredential = new Credential("rsa-test");
        rsaCredential.setType(CredentialType.RSA);
        rsaCredential.setDetail(sshKey);
        credentialDao.save(rsaCredential);

        UsernameCredentialDetail username = new UsernameCredentialDetail("user", "pass");
        usernameCredential = new Credential("username-test");
        usernameCredential.setType(CredentialType.USERNAME);
        usernameCredential.setDetail(username);
        credentialDao.save(usernameCredential);
    }

    @Test
    public void should_list_credential() {
        final ArrayList<String> names = Lists.newArrayList("rsa-test", "username-test");
        final ArrayList<CredentialType> types = Lists.newArrayList(CredentialType.RSA, CredentialType.USERNAME);

        Assert.assertEquals(2, credentialDao.list().size());
        Assert.assertEquals(2, credentialDao.list(names).size());
        Assert.assertEquals(2, credentialDao.listByType(types).size());
    }

    @Test
    public void should_get_correct_credential() {
        Credential loaded = credentialDao.get(rsaCredential.getName());

        Assert.assertNotNull(loaded);
        Assert.assertEquals(rsaCredential.getName(), loaded.getName());
        Assert.assertEquals(rsaCredential.getType(), loaded.getType());

        Assert.assertNotNull(loaded.getDetail());
        Assert.assertEquals(RSACredentialDetail.class, loaded.getDetail().getClass());
    }

    @Test
    public void should_update_credential() {
        // when:
        Credential toBeUpdated = credentialDao.get(rsaCredential.getName());
        RSACredentialDetail detail = (RSACredentialDetail) toBeUpdated.getDetail();
        detail.setPublicKey("new public key");
        credentialDao.update(toBeUpdated);

        // then:
        Credential loaded = credentialDao.get(rsaCredential.getName());
        detail = (RSACredentialDetail) loaded.getDetail();
        Assert.assertEquals("new public key", detail.getPublicKey());
    }


    @Test
    public void should_delete_success() {
        credentialDao.delete(rsaCredential);
        Assert.assertEquals(1, credentialDao.list().size());

        credentialDao.delete(usernameCredential);
        Assert.assertEquals(0, credentialDao.list().size());
    }
}
