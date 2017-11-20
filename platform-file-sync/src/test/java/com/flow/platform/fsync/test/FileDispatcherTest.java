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

package com.flow.platform.fsync.test;

import com.flow.platform.fsync.FileDispatcher;
import com.flow.platform.fsync.domain.FileSyncEvent;
import com.flow.platform.fsync.domain.FileSyncEventType;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author yang
 */
public class FileDispatcherTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private FileDispatcher dispatcher;

    @Before
    public void initFileDispatcher() throws IOException {
        File file = folder.newFolder("file-sync-test");
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        dispatcher = new FileDispatcher(file.toPath(), executorService, 10);
    }

    @Test
    public void should_parse_and_convert_file_sync_event_as_bytes() throws Throwable {
        FileSyncEvent raw = new FileSyncEvent("hello", 10L, "CHECKSUM", FileSyncEventType.CREATE);
        byte[] bytes = raw.toBytes();
        Assert.assertNotNull(bytes);

        FileSyncEvent converted = FileSyncEvent.fromBytes(bytes);
        Assert.assertNotNull(converted);
        Assert.assertEquals(raw.getServerPath(), converted.getServerPath());
        Assert.assertEquals(raw.getSize(), converted.getSize());
        Assert.assertEquals(raw.getChecksum(), converted.getChecksum());
        Assert.assertEquals(raw.getEventType(), converted.getEventType());
    }

    @Test
    public void should_init_events_if_file_exist() throws Throwable {
        // given: copy two file to watch path
        Path pathOfFirst = Paths.get(dispatcher.getWatchPath().toString(), "1.zip");
        Files.copy(getFileFromResource("1.zip"), pathOfFirst.toFile());

        Path pathOfSecond = Paths.get(dispatcher.getWatchPath().toString(), "2.zip");
        Files.copy(getFileFromResource("2.zip"), pathOfSecond.toFile());

        // when:
        dispatcher.start();

        // then:
        Assert.assertEquals(2, dispatcher.files().size());
    }

    @Test
    public void should_init_file_sync_queue() throws Throwable {
        // given:
        final String clientId = "/flow-agent/first";
        dispatcher.start();
        dispatcher.addClient(clientId);

        // when: copy two file to watch path
        Path pathOfFirst = Paths.get(dispatcher.getWatchPath().toString(), "1.zip");
        Files.copy(getFileFromResource("1.zip"), pathOfFirst.toFile());

        Path pathOfSecond = Paths.get(dispatcher.getWatchPath().toString(), "2.zip");
        Files.copy(getFileFromResource("2.zip"), pathOfSecond.toFile());

        CountDownLatch latch = new CountDownLatch(2);
        dispatcher.setFileListener(syncEvent -> {
            latch.countDown();
        });

        // then: check cached events
        latch.await(10, TimeUnit.SECONDS);
        List<FileSyncEvent> files = dispatcher.files();
        Assert.assertEquals(2, files.size());

        FileSyncEvent first = files.get(0);
        Assert.assertEquals(FileSyncEventType.CREATE, first.getEventType());
        Assert.assertEquals(pathOfFirst.toString(), first.getServerPath());
        Assert.assertEquals(222L, first.getSize().longValue());
        Assert.assertEquals("26D3901E3AE5BB42F5288AA6FE121764", first.getChecksum());

        FileSyncEvent second = files.get(1);
        Assert.assertEquals(FileSyncEventType.CREATE, second.getEventType());
        Assert.assertEquals(pathOfSecond.toString(), second.getServerPath());
        Assert.assertEquals(222L, second.getSize().longValue());
        Assert.assertEquals("84D7144283F7A9ECA89F60B178E1D24E", second.getChecksum());

        // then: check client queue
        Assert.assertEquals(2, dispatcher.getClientQueue(clientId).size());
    }

    @After
    public void clean() {
        dispatcher.stop();
        folder.delete();
    }

    private File getFileFromResource(String fileName) {
        ClassLoader classLoader = FileDispatcherTest.class.getClassLoader();
        URL resource = classLoader.getResource(fileName);
        return new File(resource.getFile());
    }
}
