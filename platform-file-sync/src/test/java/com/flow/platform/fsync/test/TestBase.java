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

import java.io.File;
import java.net.URL;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * @author yang
 */
public class TestBase {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected File getFileFromResource(String fileName) {
        ClassLoader classLoader = SyncDispatcherTest.class.getClassLoader();
        URL resource = classLoader.getResource(fileName);
        return new File(resource.getFile());
    }
}
