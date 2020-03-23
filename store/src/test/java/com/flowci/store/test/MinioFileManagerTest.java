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

import com.flowci.store.FileManager;
import com.flowci.store.MinioFileManager;
import com.flowci.store.Pathable;
import com.flowci.util.StringHelper;
import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class MinioFileManagerTest {

    private final Pathable flow = () -> "1234567890";

    private final Pathable job = () -> "10";

    private final Pathable logDir = () -> "logs";

    private final String bucket = "flows-test";

    private FileManager fileManager;

    @Before
    public void init() throws InvalidPortException, InvalidEndpointException {
        MinioClient client = new MinioClient("http://localhost:9000", "minio", "minio123");
        fileManager = new MinioFileManager(client, bucket);
    }

    @Test
    public void should_save_and_read_object() throws IOException {
        final String fileName = "test.log";
        final String content = "my-test-log";
        final Pathable[] dir = {flow, job, logDir};

        // when: save the file
        InputStream data = StringHelper.toInputStream(content);
        String logPath = fileManager.save(fileName, data, dir);
        Assert.assertNotNull(logPath);
        Assert.assertEquals("flows-test/1234567890/10/logs/test.log", logPath);

        // then: content should be read
        boolean exist = fileManager.exist(fileName, dir);
        Assert.assertTrue(exist);

        InputStream read = fileManager.read(fileName, dir);
        Assert.assertEquals(content, StringHelper.toString(read));

        // when: delete
        fileManager.remove(fileName, dir);

        // then: should throw IOException since not existed
        exist = fileManager.exist(fileName, dir);
        Assert.assertFalse(exist);
    }

    @Test(expected = IOException.class)
    public void should_throw_exception_if_not_found() throws IOException {
        fileManager.read("hello", flow, job, logDir);
    }
}
