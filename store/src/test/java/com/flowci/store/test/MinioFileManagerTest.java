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

package com.flowci.store.test;

import com.flowci.common.helper.StringHelper;
import com.flowci.store.FileManager;
import com.flowci.store.MinioFileManager;
import com.flowci.store.Pathable;
import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class MinioFileManagerTest {

    private final Pathable flow = () -> "1234567890";

    private final Pathable job = () -> "10";

    private final Pathable logDir = () -> "logs";

    private final String bucket = "flows-test";

    private FileManager fileManager;

    @BeforeEach
    void init() throws InvalidPortException, InvalidEndpointException {
        String endpoint = System.getenv().getOrDefault("FLOWCI_MINIO_ENDPOINT", "http://localhost:9000");
        String key = System.getenv().getOrDefault("FLOWCI_MINIO_KEY", "minio");
        String secret = System.getenv().getOrDefault("FLOWCI_MINIO_SECRET", "minio123");
        MinioClient client = new MinioClient(endpoint, key, secret);
        fileManager = new MinioFileManager(client, bucket);
    }

    @Test
    void should_save_and_read_object() throws IOException {
        final String fileName = "test.log";
        final String content = "my-test-log";
        final Pathable[] dir = {flow, job, logDir};

        // when: save the file
        InputStream data = StringHelper.toInputStream(content);
        String logPath = fileManager.save(fileName, data, dir);
        assertNotNull(logPath);
        assertEquals("flows-test/1234567890/10/logs/test.log", logPath);

        // then: content should be read
        boolean exist = fileManager.exist(fileName, dir);
        assertTrue(exist);

        InputStream read = fileManager.read(fileName, dir);
        assertEquals(content, StringHelper.toString(read));

        // when: delete
        fileManager.remove(fileName, dir);

        // then: should throw IOException since not existed
        exist = fileManager.exist(fileName, dir);
        assertFalse(exist);
    }

    @Test
    void should_throw_exception_if_not_found() throws IOException {
        assertThrows(IOException.class, () -> {
            fileManager.read("hello", flow, job, logDir);
        });
    }
}
