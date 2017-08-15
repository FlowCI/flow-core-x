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

import com.flow.platform.api.dao.CredentialDao;
import com.flow.platform.api.domain.Credential;
import com.flow.platform.api.service.CredentialService;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.Logger;
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
    private CredentialDao credentialDao;

    @Autowired
    private CredentialService credentialService;

    @GetMapping
    public String index() {
        return Jsonable.GSON_EXPOSE_CONFIG.toJson(credentialDao.list());
    }

    @GetMapping(path = "/{name}")
    public Credential show(@PathVariable String name) {
        return credentialService.find(name);
    }

    @PostMapping
    public Credential create(@RequestBody Credential credential) {
        return credentialService.create(credential);
    }

    @GetMapping(path = "/{name}/delete")
    public boolean delete(@PathVariable String name) {
        return credentialService.delete(name);
    }

    @PostMapping(path = "/update_credential")
    public Credential reportStatus(@RequestBody Credential credential) {
        return credentialService.update(credential);
    }

    @GetMapping(path = "/get_keys")
    public Map<String, String> getKeys() {
        return credentialService.getKeyMap();
    }
}
