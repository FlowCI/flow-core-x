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

package com.flow.platform.plugin.test;

import com.flow.platform.plugin.PluginConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

/**
 * @author gyfirim
 */

@Configuration
@Import(PluginConfig.class)
@PropertySource({"classpath:plugin.properties"})
public class AppConfig {

    public Path folder;

    @PostConstruct
    protected void init() throws IOException {
        folder = Files.createDirectories(Paths.get("/tmp", "/test" + new Random(1000).toString()));
    }

    @PreDestroy
    protected void destroy() throws IOException {
        FileUtils.deleteDirectory(folder.toFile());
    }
}
