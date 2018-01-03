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

import com.flow.platform.api.domain.Storage;
import com.flow.platform.api.service.StorageService;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author yh@firim
 */
@RestController
@RequestMapping(path = "/storages")
public class StorageController {

    @Autowired
    private StorageService storageService;

    @PostMapping
    public Storage post(@RequestPart(name = "file") MultipartFile file) {
        return storageService.create(file);
    }

    @GetMapping(path = "/{id}")
    public Resource get(@PathVariable String id, HttpServletResponse httpResponse) {

        Storage storage = storageService.get(id);
        httpResponse.setHeader(
            "Content-Disposition",
            String.format("attachment; filename=%s", storage.getName()));
        return storageService.getResource(id);
    }
}
