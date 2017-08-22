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

import com.flow.platform.api.domain.Credential;
import com.flow.platform.api.service.CredentialService;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.Logger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lhl
 */

@RestController
@RequestMapping(path = "/credentials")
public class CredentialController {

    private final static Logger LOGGER = new Logger(CredentialController.class);

    @Autowired
    private CredentialService credentialService;

    @GetMapping
    public List<Credential> index() {
        return credentialService.listCredentials();
    }

    @GetMapping(path = "/{name}")
    public String show(@PathVariable String name) {
        Credential credential = credentialService.find(name);
        return credential.toJson();
    }

    @PostMapping
    public Object create(@RequestBody String credentialJson) {
        Credential credential = Jsonable.GSON_CONFIG.fromJson(credentialJson, Credential.class);
        Object o = Jsonable.GSON_CONFIG.fromJson(credentialJson, credential.getCredentialType().getClazz());
        return credentialService.create((Credential) o);
    }

    @GetMapping(path = "/{name}/delete")
    public void delete(@PathVariable String name) {
        credentialService.delete(name);
    }

    @PostMapping(path = "/{name}/update")
    public Object reportStatus(@RequestBody String credentialJson) {
        Credential credential = Jsonable.GSON_CONFIG.fromJson(credentialJson, Credential.class);
        Object o = Jsonable.GSON_CONFIG.fromJson(credentialJson, credential.getCredentialType().getClazz());
        return credentialService.update((Credential) o);
    }

    @GetMapping(path = "/ssh/keys")
    public Map<String, String> getKeys() {
        return credentialService.getKeyMap();
    }

    @GetMapping(path = "{credentialType}/credentialTypeList")
    public Collection<Credential> credentialTypeList(@PathVariable String credentialType) {
        return credentialService.listTypes(credentialType);
    }
}
