/*
 * Copyright 2018 flow.ci
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

package com.flowci.util;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author yang
 */
public abstract class FileHelper {

    private static final int BufferSize = 4096;

    public static Path createDirectory(Path dir) throws IOException {
        try {
            return Files.createDirectories(dir);
        } catch (FileAlreadyExistsException ignore) {
            return dir;
        }
    }

    public static void writeToFile(InputStream src, Path destFile) throws IOException {
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(destFile.toFile()))) {
            byte[] buffer = new byte[BufferSize];
            int read;
            while ((read = src.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    public static String getName(String file) {
        checkNotNull(file);
        String fileName = new File(file).getName();
        int dotIndex = fileName.indexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    public static void unzip(InputStream src, Path destDir) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(src)) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                Path filePath = Paths.get(destDir.toString(), entry.getName());
                if (!entry.isDirectory()) {
                    writeToFile(zipIn, filePath);
                } else {
                    filePath.toFile().mkdirs();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

}
