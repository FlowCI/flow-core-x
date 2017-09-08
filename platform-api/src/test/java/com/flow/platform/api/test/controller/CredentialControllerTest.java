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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.domain.credential.AndroidCredentialDetail;
import com.flow.platform.api.domain.credential.Credential;
import com.flow.platform.api.domain.credential.CredentialDetail;
import com.flow.platform.api.domain.credential.CredentialType;
import com.flow.platform.api.domain.credential.IosCredentialDetail;
import com.flow.platform.api.domain.credential.RSACredentialDetail;
import com.flow.platform.api.domain.credential.RSAKeyPair;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yang
 */
public class CredentialControllerTest extends TestBase {

    @Autowired
    private CredentialService credentialService;

    @Before
    public void initCredentialOfTypes() {
        RSACredentialDetail rsaDetail = new RSACredentialDetail("public key", "private key");
        credentialService.createOrUpdate("rsa-credential", rsaDetail);

        UsernameCredentialDetail usernameDetail = new UsernameCredentialDetail("user", "pass");
        credentialService.createOrUpdate("username-credential", usernameDetail);

        AndroidCredentialDetail androidDetail = new AndroidCredentialDetail();
        androidDetail.setFile(new FileResource("android.jks", "/path/of/android.jks"));
        androidDetail.setKeyStorePassword("12345");
        androidDetail.setKeyStoreAlias("android");
        androidDetail.setKeyStoreAliasPassword("12345");
        credentialService.createOrUpdate("android-credential", androidDetail);

        IosCredentialDetail iosDetail = new IosCredentialDetail();
        iosDetail.setProvisionProfiles(Lists.newArrayList(new FileResource("pp", "pp.pp")));
        iosDetail.setP12s(Lists.newArrayList(new PasswordFileResource("p12", "p12.p12", "12345")));
        credentialService.createOrUpdate("ios-credential", iosDetail);
    }

    @Test
    public void should_list_credential_without_type() throws Throwable {
        String response = performRequestWith200Status(get("/credentials"));
        Credential[] credentials = Jsonable.parseArray(response, Credential[].class);
        Assert.assertEquals(4, credentials.length);
    }

    @Test
    public void should_list_credential_with_type_param() throws Throwable {
        String response = performRequestWith200Status(get("/credentials?types=android,rsa,ios"));
        Credential[] credentials = Jsonable.parseArray(response, Credential[].class);
        Assert.assertEquals(3, credentials.length);
    }

    @Test
    public void should_create_rsa_credential() throws Throwable {
        // to generate rsa key pair
        String response = performRequestWith200Status(get("/credentials/rsa"));
        RSAKeyPair pair = RSAKeyPair.parse(response, RSAKeyPair.class);
        Assert.assertNotNull(pair.getPublicKey());
        Assert.assertNotNull(pair.getPublicKey());

        // build mock detail entity

        // when: mock create rsa credential
        final String credentialName = "rsa-test";

        performRequestWith200Status(
            post(getUrlForCredential(credentialName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new RSACredentialDetail(pair).toJson())
        );

        // then: load by name
        response = performRequestWith200Status(get(getUrlForCredential(credentialName)));
        Credential rsaCredential = Credential.parse(response, Credential.class);
        Assert.assertNotNull(rsaCredential);

        // then: verify credential
        Assert.assertEquals(credentialName, rsaCredential.getName());
        Assert.assertEquals(CredentialType.RSA, rsaCredential.getType());

        CredentialDetail detail = rsaCredential.getDetail();
        Assert.assertTrue(detail instanceof RSACredentialDetail);

        RSACredentialDetail rsaDetail = (RSACredentialDetail) detail;
        Assert.assertEquals(pair.getPublicKey(), rsaDetail.getPublicKey());
        Assert.assertEquals(pair.getPrivateKey(), rsaDetail.getPrivateKey());
    }

    @Test
    public void should_create_rsa_credential_auto_generate() throws Throwable {
        // when: mock create rsa credential without rsa key pair
        final String credentialName = "rsa-test";

        performRequestWith200Status(post(getUrlForCredential(credentialName))
            .contentType(MediaType.APPLICATION_JSON)
            .content(new RSACredentialDetail().toJson())
        );

        // then: load by name
        String response = performRequestWith200Status(get(getUrlForCredential(credentialName)));
        Credential rsaCredential = Credential.parse(response, Credential.class);
        Assert.assertNotNull(rsaCredential);

        // then: verify credential
        Assert.assertEquals(credentialName, rsaCredential.getName());
        Assert.assertEquals(CredentialType.RSA, rsaCredential.getType());

        CredentialDetail detail = rsaCredential.getDetail();
        Assert.assertTrue(detail instanceof RSACredentialDetail);

        RSACredentialDetail rsaDetail = (RSACredentialDetail) detail;
        Assert.assertNotNull(rsaDetail.getPublicKey());
        Assert.assertNotNull(rsaDetail.getPrivateKey());
    }

    @Test
    public void should_create_username_credential() throws Throwable {
        // given:
        final String credentialName = "username-test";

        // when: create username credential
        performRequestWith200Status(
            post(getUrlForCredential(credentialName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(new UsernameCredentialDetail("username", "password").toJson()));

        // then:
        String response = performRequestWith200Status(get(getUrlForCredential(credentialName)));
        Credential credential = Credential.parse(response, Credential.class);
        Assert.assertNotNull(credential);
        Assert.assertEquals(CredentialType.USERNAME, credential.getType());

        CredentialDetail detail = credential.getDetail();
        Assert.assertEquals(UsernameCredentialDetail.class, detail.getClass());

        UsernameCredentialDetail usernameDetail = (UsernameCredentialDetail) detail;
        Assert.assertEquals("username", usernameDetail.getUsername());
        Assert.assertEquals("password", usernameDetail.getPassword());
    }

    @Test
    public void should_create_android_credential() throws Throwable {
        // given:
        final String credentialName = "android-test";

        // when: create android credential
        AndroidCredentialDetail detail = new AndroidCredentialDetail();
        detail.setKeyStoreAliasPassword("12345");
        detail.setKeyStoreAlias("alias");
        detail.setKeyStorePassword("54321");

        performRequestWith200Status(fileUpload(getUrlForCredential(credentialName))
            .file(createDetailPart(detail))
            .file(createAndroidFilePart("file-name.jks"))
        );

        // then:
        String response = performRequestWith200Status(get(getUrlForCredential(credentialName)));
        Credential credential = Credential.parse(response, Credential.class);
        Assert.assertNotNull(credential);

        Assert.assertEquals(credentialName, credential.getName());
        Assert.assertEquals(CredentialType.ANDROID, credential.getType());
        Assert.assertEquals(AndroidCredentialDetail.class, credential.getDetail().getClass());

        AndroidCredentialDetail androidDetail = (AndroidCredentialDetail) credential.getDetail();
        Assert.assertEquals("12345", androidDetail.getKeyStoreAliasPassword());
        Assert.assertEquals("alias", androidDetail.getKeyStoreAlias());
        Assert.assertEquals("54321", androidDetail.getKeyStorePassword());

        Assert.assertNotNull(androidDetail.getFile());
        Assert.assertEquals("file-name.jks", androidDetail.getFile().getName());
        Assert.assertNull(androidDetail.getFile().getPath()); // actual path cannot to client
    }

    @Test
    public void should_create_ios_credential() throws Throwable {
        // given: create ios credential
        final String credentialName = "ios-test";

        // set password for p12-3.p12 file
        IosCredentialDetail detail = new IosCredentialDetail();
        detail.getP12s().add(new PasswordFileResource("p12-3.p12", null, "123123"));

        performRequestWith200Status(fileUpload(getUrlForCredential(credentialName))
            .file(createDetailPart(detail))
            .file(createIosP12Part("p12-1.p12"))
            .file(createIosP12Part("p12-2.p12"))
            .file(createIosP12Part("p12-3.p12"))
            .file(createIosProvisionProfilePart("pp1.mobileprovision"))
            .file(createIosProvisionProfilePart("pp2.mobileprovision")));

        // when: load credential after created
        String response = performRequestWith200Status(get(getUrlForCredential(credentialName)));
        Credential credential = Credential.parse(response, Credential.class);
        Assert.assertNotNull(credential);

        // then:
        Assert.assertEquals(credentialName, credential.getName());
        Assert.assertEquals(CredentialType.IOS, credential.getType());
        Assert.assertEquals(IosCredentialDetail.class, credential.getDetail().getClass());

        IosCredentialDetail iosDetail = (IosCredentialDetail) credential.getDetail();
        Assert.assertEquals(3, iosDetail.getP12s().size());
        Assert.assertEquals(2, iosDetail.getProvisionProfiles().size());

        for (PasswordFileResource resource : iosDetail.getP12s()) {
            if (resource.getName().equals("p12-3.p12")) {
                Assert.assertEquals("123123", resource.getPassword());
            }
        }
    }

    private String getUrlForCredential(String credentialName) {
        return "/credentials/" + credentialName;
    }

    private MockMultipartFile createDetailPart(CredentialDetail detail) {
        return new MockMultipartFile("detail", "", "application/json", detail.toBytes());
    }

    private MockMultipartFile createAndroidFilePart(String name) {
        return new MockMultipartFile("android-file", name, "application/jks", "content".getBytes());
    }

    private MockMultipartFile createIosProvisionProfilePart(String fileName) {
        return new MockMultipartFile("pp-files", fileName, "application/pp", "content".getBytes());
    }

    private MockMultipartFile createIosP12Part(String fileName) {
        return new MockMultipartFile("p12-files", fileName, "application/pp", "content".getBytes());
    }
}
