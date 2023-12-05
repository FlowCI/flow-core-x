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
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;

@Configuration
public class StorageConfig {

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private AppProperties.Minio minioProperties;

    @Bean("fileManager")
    @ConditionalOnProperty(name = "app.minio.enabled", havingValue = "true")
    public FileManager minioFileManager(MinioClient client) {
        return new MinioFileManager(client, minioProperties.getBucket());
    }

    @Bean
    @ConditionalOnProperty(name = "app.minio.enabled", havingValue = "true")
    public MinioClient minioClient() {
        URL endpoint = minioProperties.getEndpoint();
        String key = minioProperties.getKey();
        String secret = minioProperties.getSecret();

        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(key, secret)
                .build();
    }

    @Bean("fileManager")
    @ConditionalOnMissingBean(FileManager.class)
    public FileManager localFileManager() {
        return new LocalFileManager(appProperties.getFlowDir());
    }
}
