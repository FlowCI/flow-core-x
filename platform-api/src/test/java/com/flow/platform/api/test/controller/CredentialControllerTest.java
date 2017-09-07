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

package com.flow.platform.api.test.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.domain.credential.AndroidCredentialDetail;
import com.flow.platform.api.domain.credential.Credential;
import com.flow.platform.api.domain.credential.IosCredentialDetail;
import com.flow.platform.api.domain.credential.RSACredentialDetail;
import com.flow.platform.api.domain.credential.UsernameCredentialDetail;
import com.flow.platform.api.domain.file.FileResource;
import com.flow.platform.api.domain.file.PasswordFileResource;
import com.flow.platform.api.service.CredentialService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.domain.Jsonable;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * @author yang
 */
public class CredentialControllerTest extends TestBase {

    @Autowired
    private CredentialService credentialService;

    @Before
    public void initCredentialOfTypes() {
        RSACredentialDetail rsaDetail = new RSACredentialDetail("public key", "private key");
        credentialService.create("rsa-credential", rsaDetail);

        UsernameCredentialDetail usernameDetail = new UsernameCredentialDetail("user", "pass");
        credentialService.create("username-credential", usernameDetail);

        AndroidCredentialDetail androidDetail = new AndroidCredentialDetail();
        androidDetail.setFile(new FileResource("android.jks", "/path/of/android.jks"));
        androidDetail.setKeyStorePassword("12345");
        androidDetail.setKeyStoreAlias("android");
        androidDetail.setKeyStoreAliasPassword("12345");
        credentialService.create("android-credential", androidDetail);

        IosCredentialDetail iosDetail = new IosCredentialDetail();
        iosDetail.setProvisionProfiles(Lists.newArrayList(new FileResource("pp", "pp.pp")));
        iosDetail.setP12s(Lists.newArrayList(new PasswordFileResource("p12", "p12.p12", "12345")));
        credentialService.create("ios-credential", iosDetail);
    }

    @Test
    public void should_list_credential_without_type() throws Throwable {
        MvcResult result = mockMvc.perform(get("/credentials")).andExpect(status().isOk()).andReturn();

        Credential[] credentials = Jsonable.parseArray(result.getResponse().getContentAsString(), Credential[].class);

        Assert.assertEquals(4, credentials.length);
    }

    @Test
    public void should_list_credential_with_type_param() throws Throwable {
        MvcResult result = mockMvc.perform(get("/credentials?types=android,rsa,ios"))
            .andExpect(status().isOk())
            .andReturn();

        Credential[] credentials = Jsonable.parseArray(result.getResponse().getContentAsString(), Credential[].class);

        Assert.assertEquals(3, credentials.length);
    }

}
