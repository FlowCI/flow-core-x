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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.flow.platform.fsync.SyncClient;
import com.flow.platform.fsync.domain.FileSyncEvent;
import com.flow.platform.fsync.domain.FileSyncEventType;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author yang
 */
public class SyncClientTest extends TestBase {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @Before
    public void mockDownloadUrl() throws IOException {
        wireMockRule.resetAll();

        File fileFromResource = getFileFromResource("1.zip");
        stubFor(get(urlEqualTo("/file/sync/26D3901E3AE5BB42F5288AA6FE121764"))
            .willReturn(aResponse().withBody(Files.readAllBytes(fileFromResource.toPath()))));
    }

    @Test
    public void should_enable_to_sync_file() throws Throwable {
        File folder = this.folder.newFolder("client-sync");
        SyncClient syncClient = new SyncClient("http://localhost:8080/file/sync", folder.toPath());

        FileSyncEvent event = new FileSyncEvent(
            "/etc/server/1.zip", 222L, "26D3901E3AE5BB42F5288AA6FE121764", FileSyncEventType.CREATE);
        boolean isSynced = syncClient.onSyncEvent(event);

        Assert.assertTrue(isSynced);
        Assert.assertTrue(Paths.get(folder.getAbsolutePath(), "1.zip").toFile().exists());
    }
}
