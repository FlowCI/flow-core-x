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

package com.flow.platform.api.service;

import com.flow.platform.api.dao.LocalFileResourceDao;
import com.flow.platform.api.domain.LocalFileResource;
import com.flow.platform.api.util.CommonUtil;
import com.flow.platform.core.exception.FlowException;
import com.flow.platform.util.CommandUtil.Unix;
import com.flow.platform.util.Logger;
import com.google.common.io.Files;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author yh@firim
 */
@Service
public class LocalFileResourceServiceImpl implements LocalFileResourceService {

    private final static String STORAGE_FOLDER = "storages";

    private final static String DOT = ".";

    private final static Logger LOGGER = new Logger(LocalFileResource.class);

    private final static String STORAGE = "storages";

    @Autowired
    private Path workspace;

    @Autowired
    private LocalFileResourceDao localFileResourceDao;

    @Value("${domain.api}")
    private String apiHost;

    @Override
    public LocalFileResource get(String id) {
        LocalFileResource storage = localFileResourceDao.get(id);
        if (Objects.isNull(storage)) {
            throw new FlowException("Not found storage");
        }

        return storage;
    }

    @Override
    public LocalFileResource save(LocalFileResource storage) {
        localFileResourceDao.save(storage);
        return storage;
    }

    @Override
    public LocalFileResource update(LocalFileResource storage) {
        localFileResourceDao.update(storage);
        return storage;
    }

    @Override
    public LocalFileResource delete(LocalFileResource storage) {
        localFileResourceDao.delete(storage);
        return storage;
    }

    @Override
    public LocalFileResource create(MultipartFile file) {

        String name = file.getOriginalFilename();
        String extension = Files.getFileExtension(name);
        String id = CommonUtil.randomId().toString();

        LocalFileResource storage = new LocalFileResource(name, id, extension);
        storage.setCreatedAt(ZonedDateTime.now());
        localFileResourceDao.save(storage);
        doSaveFile(file, storage);

        storage.setUrl(buildDownloadUrl(storage));
        return storage;
    }

    @Override
    public Resource getResource(String id) {
        Resource resource;
        LocalFileResource storage = localFileResourceDao.get(id);
        Path destPath = buildPath(storage);

        if (!destPath.toFile().exists()) {
            throw new FlowException("Not found storage file");
        }

        try {
            InputStream inputStream = new FileInputStream(destPath.toFile());
            resource = new InputStreamResource(inputStream);
        } catch (FileNotFoundException e) {
            throw new FlowException("Not found storage file " + e.getMessage());
        }

        return resource;
    }

    private void doSaveFile(MultipartFile file, LocalFileResource storage) {
        Path destPath = buildPath(storage);
        try {
            file.transferTo(destPath.toFile());
        } catch (IOException e) {
            LOGGER.error("Save storage exception", e);
        }
    }

    private Path buildPath(LocalFileResource storage) {
        Path storageFolder = Paths.get(workspace.toString(), STORAGE_FOLDER);
        if (!storageFolder.toFile().exists()) {
            try {
                java.nio.file.Files.createDirectories(storageFolder);
            } catch (IOException e) {
                LOGGER.error("Create storage folders exception", e);
            }
        }

        return Paths.get(storageFolder.toString(), storage.getId() + DOT + storage.getExtension());
    }

    private String buildDownloadUrl(LocalFileResource storage) {
        return apiHost + Unix.PATH_SEPARATOR + STORAGE + Unix.PATH_SEPARATOR + storage.getId();
    }
}
