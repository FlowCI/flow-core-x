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

import com.flow.platform.plugin.service.PluginService;
import com.flow.platform.plugin.service.PluginStoreService;
import java.nio.file.Path;
import org.junit.FixMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author gyfirim
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {AppConfig.class})
@FixMethodOrder(MethodSorters.JVM)
public abstract class TestBase {

    @Autowired
    protected PluginService pluginService;

    @Autowired
    protected PluginStoreService pluginStoreService;

    // git clone folder
    @Autowired
    protected Path gitWorkspace;

    // local library
    @Autowired
    protected Path gitCacheWorkspace;

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected ThreadPoolTaskExecutor pluginPoolExecutor;
}
