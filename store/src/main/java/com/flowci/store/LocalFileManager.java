/*
 * Copyright 2019 flow.ci
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

package com.flowci.store;

import com.flowci.common.helper.FileHelper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Local file system storage manager,
 * instance created on @see com.flowci.core.common.config.StorageConfig
 */
public class LocalFileManager implements FileManager {

    private final Path base;

    public LocalFileManager(Path base) {
        this.base = base;
    }

    @Override
    public String create(Pathable... objs) throws IOException {
        Path dir = connect(base, objs);
        return FileHelper.createDirectory(dir).toString();
    }

    @Override
    public boolean exist(Pathable... objs) {
        Path dir = connect(base, objs);
        return Files.exists(dir);
    }

    @Override
    public boolean exist(String fileName, Pathable... objs) {
        Path dir = connect(base, objs);
        Path filePath = Paths.get(dir.toString(), fileName);
        return Files.exists(filePath);
    }

    @Override
    public String save(String fileName, InputStream data, Pathable... objs) throws IOException {
        Path dir = connect(base, objs);
        if (!Files.exists(dir)) {
            create(objs);
        }

        Path filePath = Paths.get(dir.toString(), fileName);
        Files.copy(data, filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath.toString();
    }

    @Override
    public InputStream read(String fileName, Pathable... objs) throws IOException {
        Path dir = connect(base, objs);
        Path target = Paths.get(dir.toString(), fileName);

        if (!Files.exists(target)) {
            throw new IOException("File not found");
        }

        return new FileInputStream(target.toFile());
    }

    @Override
    public String remove(String fileName, Pathable... objs) throws IOException {
        Path dir = connect(base, objs);
        Path target = Paths.get(dir.toString(), fileName);
        Files.deleteIfExists(target);
        return target.toString();
    }

    @Override
    public String remove(String filePath) throws IOException {
        Path target = Paths.get(filePath);
        Files.deleteIfExists(target);
        return filePath;
    }

    private static Path connect(Path base, Pathable... objs) {
        Path path = base;

        for (Pathable item : objs) {
            path = Paths.get(path.toString(), item.pathName());
        }

        return path;
    }
}
