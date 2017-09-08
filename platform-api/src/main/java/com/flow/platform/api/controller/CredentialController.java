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
package com.flow.platform.api.controller;

import com.flow.platform.api.domain.credential.AndroidCredentialDetail;
import com.flow.platform.api.domain.credential.Credential;
import com.flow.platform.api.domain.credential.CredentialDetail;
import com.flow.platform.api.domain.credential.CredentialType;
import com.flow.platform.api.domain.credential.IosCredentialDetail;
import com.flow.platform.api.domain.credential.RSAKeyPair;
import com.flow.platform.api.domain.file.FileResource;
import com.flow.platform.api.domain.file.PasswordFileResource;
import com.flow.platform.api.service.CredentialService;
import com.flow.platform.core.exception.IllegalParameterException;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.util.Logger;
import com.flow.platform.util.StringUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author lhl
 */

@RestController
@RequestMapping(path = "/credentials")
public class CredentialController {

    private final static Logger LOGGER = new Logger(CredentialController.class);

    private final static Set<String> ANDROID_EXTENSIONS = Sets.newHashSet("jks");

    private final static Set<String> IOS_PROVISION_PROFILE_EXTENSIONS = Sets.newHashSet("mobileprovision");

    private final static Set<String> IOS_P12_EXTENSIONS = Sets.newHashSet("p12");

    private final static SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private Path workspace;

    /**
     * @api {get} /credentials List
     * @apiParam {String="android","rsa","ios","username"} types Passed as ?types=android,ios,username
     * @apiGroup Credenital
     * @apiDescription List credentials
     *
     * @apiSuccessExample {json} Success-Response
     *  [
            {
                "name": "android-credential",
                "type": "ANDROID",
                "detail": {
                    "file": {
                        "name": "android.jks"
                    },
                    "keyStorePassword": "12345",
                    "keyStoreAlias": "android",
                    "keyStoreAliasPassword": "12345"
                },
                "createdAt": 1504737923,
                "updatedAt": 1504737923
            },

            {
                "name": "ios-credential",
                "type": "IOS",
                "detail": {
                    "provisionProfiles": [
                        {
                            "name": "pp"
                        }
                    ],

                    "p12s": [
                        {
                            "password": "12345",
                            "name": "p12"
                        }
                    ]
                },
                "createdAt": 1504737923,
                "updatedAt": 1504737923
            },

            {
                "name": "ras-credential",
                "type": "RSA",
                "detail": {
                    "publicKey": "public key",
                    "privateKey": "private key"
                },
                "createdAt": 1504737923,
                "updatedAt": 1504737923
            },

            {
                "name": "username-credential",
                "type": "USERNAME",
                "detail": {
                    "username": "user",
                    "password": "pass"
                },
                "createdAt": 1504737923,
                "updatedAt": 1504737923
            }
        ]
     */
    @GetMapping
    public List<Credential> list(@RequestParam(required = false) String types) {
        final Set<CredentialType> typeSet = new HashSet<>(CredentialType.values().length);

        if (!Strings.isNullOrEmpty(types)) {
            types = types.trim();

            ControllerUtil.extractParam(types, input -> {
                typeSet.add(CredentialType.valueOf(input.toUpperCase()));
                return null;
            });
        }

        return credentialService.list(typeSet);
    }

    /**
     * @api {get} /credentials/:name Show
     * @apiParam {String} name Credential name
     * @apiGroup Credenital
     * @apiDescription Get credential by name
     *
     * @apiSuccessExample {json} Success-Response
     *
     *  reference on List item
     */
    @GetMapping(path = "/{name}")
    public Credential show(@PathVariable String name) {
        return credentialService.find(name);
    }

    /**
     * @api {post} /credentials/:type CreateOrUpdate with Multipart
     * @apiParam {String="android","rsa","ios","username"} type Credential type
     * @apiParamExample {multipart} Request-Body:
     *  - part:
     *      - name: detail
     *      - content-type: application/json
     *  - part:
     *      -
     *
     *
     * @apiGroup Credenital
     * @apiDescription Create credentials with content-type multipart/form-data
     *
     * @apiSuccessExample {json} Success-Response
     *
     *  reference on List item
     */
    @PostMapping(path = "/{name}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Credential createOrUpdate(@PathVariable String name,
                                     @RequestPart(name = "detail") CredentialDetail detail,
                                     @RequestPart(name = "android-file", required = false) MultipartFile androidFile,
                                     @RequestPart(name = "p12-files", required = false) MultipartFile[] p12Files,
                                     @RequestPart(name = "pp-files", required = false) MultipartFile[] ppFiles) {

        try {
            handleAndroidFile(detail, androidFile);
            handleIosFile(detail, p12Files, ppFiles);

            Credential credential = credentialService.createOrUpdate(name, detail);
            return credential;
        } catch (IOException e) {
            throw new IllegalStatusException("Cannot save credential file with io error: " + e.getMessage());
        }
    }

    /**
     * @api {post} /credentials/:type CreateOrUpdate with Json
     * @apiParam {String="android","rsa","ios","username"} type Credential type
     * @apiParamExample {json} Request-Body:
     *
     *  reference on List item
     * @apiGroup Credenital
     * @apiDescription Create credentials with content-type application/json
     *
     * @apiSuccessExample {json} Success-Response
     *
     *  reference on List item
     */
    @PostMapping(path = "/{name}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Credential createOrUpdate(@PathVariable String name,
                                     @RequestBody CredentialDetail detail) {

        Credential credential = credentialService.createOrUpdate(name, detail);
        return credential;
    }

    /**
     * @api {delete} /credentials/:name Delete
     * @apiParam {String} name Credential name
     * @apiGroup Credenital
     * @apiDescription Delete credential
     */
    @DeleteMapping(path = "/{name}")
    public void delete(@PathVariable String name) {
        credentialService.delete(name);
    }

    /**
     * @api {get} /credentials/rsa Gen Rsa Key Pari
     * @apiGroup Credenital
     * @apiDescription Generate RSA key pair
     *
     * @apiSuccessExample {json} Success-Response
     *  {
     *      privateKey: xxx,
     *      publicKey: xxx
     *  }
     */
    @GetMapping(path = "/rsa")
    public RSAKeyPair getKeys() {
        return credentialService.generateRsaKey();
    }

    private void handleAndroidFile(CredentialDetail detail, MultipartFile file) throws IOException {
        if (!(detail instanceof AndroidCredentialDetail)) {
            return;
        }

        if (file == null || file.isEmpty()) {
            return;
        }

        AndroidCredentialDetail androidDetail = (AndroidCredentialDetail) detail;
        String extension = Files.getFileExtension(file.getOriginalFilename());

        if (!ANDROID_EXTENSIONS.contains(extension)) {
            throw new IllegalParameterException("Illegal android cert file");
        }

        String destFileName = getFileName(file.getOriginalFilename());
        Path destPath = credentailFilePath(destFileName);

        file.transferTo(destPath.toFile());
        androidDetail.setFile(new FileResource(file.getOriginalFilename(), destPath.toString()));
    }

    private void handleIosFile(CredentialDetail detail,
                               MultipartFile[] p12Files,
                               MultipartFile[] ppFiles) throws IOException {
        if (!(detail instanceof IosCredentialDetail)) {
            return;
        }

        IosCredentialDetail iosDetail = (IosCredentialDetail) detail;

        for(MultipartFile file : p12Files) {
            String extension = Files.getFileExtension(file.getOriginalFilename());
            if (!IOS_P12_EXTENSIONS.contains(extension)) {
                throw new IllegalParameterException("Illegal ios p12 file");
            }

            PasswordFileResource resource = null;

            // find detail has valid password
            for (PasswordFileResource item : iosDetail.getP12s()) {
                if (Strings.isNullOrEmpty(item.getName())) {
                    continue;
                }

                if (item.getName().equals(file.getOriginalFilename())) {
                    resource = item;
                    break;
                }
            }

            Path destPath = credentailFilePath(getFileName(file.getOriginalFilename()));
            file.transferTo(destPath.toFile());

            if (resource == null) {
                iosDetail.getP12s().add(
                    new PasswordFileResource(file.getOriginalFilename(), destPath.toString(), StringUtil.EMPTY));
                continue;
            }

            resource.setName(file.getOriginalFilename());
            resource.setPath(destPath.toString());
        }

        for(MultipartFile file : ppFiles) {
            String extension = Files.getFileExtension(file.getOriginalFilename());
            if (!IOS_PROVISION_PROFILE_EXTENSIONS.contains(extension)) {
                throw new IllegalParameterException("Illegal ios mobile provision file");
            }

            Path destPath = credentailFilePath(getFileName(file.getOriginalFilename()));
            file.transferTo(destPath.toFile());

            FileResource resource = new FileResource(file.getOriginalFilename(), destPath.toString());
            iosDetail.getProvisionProfiles().add(resource);
        }
    }

    private Path credentailFilePath(String fileName) throws IOException {
        Path path = Paths.get(workspace.toString(), "credentials", fileName);
        Files.createParentDirs(path.toFile());
        return path;
    }

    private String getFileName(String originalFilename) {
        return String.format("%s-%s.%s",
            Files.getNameWithoutExtension(originalFilename),
            FILE_DATE_FORMAT.format(new Date()),
            Files.getFileExtension(originalFilename));
    }
}
