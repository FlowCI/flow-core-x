/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.common.helper;

import com.flowci.common.helper.FileHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileHelperTest {

    @TempDir
    private File temp;

    @Test
    void should_get_file_name_without_type() {
        String name = FileHelper.getName("hello.log.raw");
        Assertions.assertEquals("hello", name);
    }

    @Test
    void should_unzip_file() throws IOException {
        InputStream src = this.getClass().getClassLoader().getResourceAsStream("jacoco-report.zip");
        Assertions.assertNotNull(src);

        Path destDir = Files.createDirectories(Paths.get(temp.getPath(), "jacoco-report"));;
        FileHelper.unzip(src, destDir);

        assertTrue(Files.exists(destDir));
    }

    @Test
    void should_verify_path_is_start_from_root() {
        assertTrue(FileHelper.isStartWithRoot("/ws/abc"));
        assertTrue(FileHelper.isStartWithRoot("C:\\ws\\abc"));

        assertFalse(FileHelper.isStartWithRoot("./ws/abc"));
        assertFalse(FileHelper.isStartWithRoot(".\\ws\\abc"));
    }

    @Test
    public void should_verify_path_has_overlap_or_duplication() {
        assertTrue(FileHelper.hasOverlapOrDuplicatePath(List.of(
                "./ws/abc",
                "./ws"
        )));

        assertTrue(FileHelper.hasOverlapOrDuplicatePath(List.of(
                "./ws/abc",
                "./ws/abc/"
        )));

        assertFalse(FileHelper.hasOverlapOrDuplicatePath(List.of(
                "test",
                "ws/abc",
                "abc/efg"
        )));
    }
}
