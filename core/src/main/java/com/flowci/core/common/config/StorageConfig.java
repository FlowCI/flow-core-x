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

package com.flowci.core.common.config;

import com.flowci.store.FileManager;
import com.flowci.store.LocalFileManager;
import com.flowci.store.MinioFileManager;
import com.flowci.util.FileHelper;
import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

@Configuration
public class StorageConfig {

    @Autowired
    private ConfigProperties appProperties;

    @Autowired
    private ConfigProperties.Minio minioProperties;

    @PostConstruct
    private void initFlowDir() throws IOException {
        Path path = appProperties.getFlowDir();
        FileHelper.createDirectory(path);
    }

    @Bean("fileManager")
    @ConditionalOnProperty(name = "app.minio.enabled", havingValue = "true")
    public FileManager minioFileManager(MinioClient client) {
        return new MinioFileManager(client, minioProperties.getBucket());
    }

    @Bean
    @ConditionalOnProperty(name = "app.minio.enabled", havingValue = "true")
    public MinioClient minioClient() throws InvalidPortException, InvalidEndpointException {
        URL endpoint = minioProperties.getEndpoint();
        String key = minioProperties.getKey();
        String secret = minioProperties.getSecret();
        return new MinioClient(endpoint, key, secret);
    }

    @Bean("fileManager")
    @ConditionalOnMissingBean(FileManager.class)
    public FileManager localFileManager() {
        return new LocalFileManager(appProperties.getFlowDir());
    }
}
