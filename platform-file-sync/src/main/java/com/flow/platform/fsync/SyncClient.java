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

package com.flow.platform.fsync;

import com.flow.platform.fsync.domain.FileSyncEvent;
import com.flow.platform.util.FileUtil;
import com.flow.platform.util.Logger;
import com.flow.platform.util.ObjectWrapper;
import com.flow.platform.util.http.HttpClient;
import com.flow.platform.util.http.HttpURL;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author yang
 */
public class SyncClient {

    private final static Logger LOGGER = new Logger(SyncClient.class);

    private final String baseUrl;

    private final Path syncFolder;

    public SyncClient(String baseUrl, Path syncFolder) {
        this.baseUrl = baseUrl;
        this.syncFolder = syncFolder;
    }

    public boolean onSyncEvent(FileSyncEvent event) {
        HttpURL url = HttpURL.build(baseUrl).append(event.getChecksum());
        ObjectWrapper<Boolean> result = new ObjectWrapper<>(false);

        // download form server
        HttpClient httpClient = HttpClient.build(url.toString()).get();
        httpClient.bodyAsStream(response -> {
            if (!response.hasSuccess()) {
                result.setInstance(false);
                return;
            }

            try {
                // save to local folder
                File raw = new File(event.getServerPath());
                Path target = Paths.get(syncFolder.toString(), raw.getName());
                FileUtil.write(response.getBody(), target);

                // verify size and check sum
                if (Files.size(target) == event.getSize()) {
                    HashCode hash = com.google.common.io.Files.hash(target.toFile(), Hashing.md5());
                    if (hash.toString().toUpperCase().equals(event.getChecksum())) {
                        result.setInstance(true);
                    }
                }

            } catch (IOException e) {
                LOGGER.warn("Unable to write file: " + e.getMessage());
            }
        });

        return result.getInstance();
    }
}
