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

package com.flow.platform.cc.test.util;

import com.flow.platform.cc.resource.PropertyResourceLoader;
import com.flow.platform.cc.test.EmptyConfig;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import mockit.Mock;
import mockit.MockUp;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author yang
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = EmptyConfig.class)
public class PropertyResourceLoaderTest {

    private PropertyResourceLoader propertyResourceLoader = new PropertyResourceLoader();

    final Map<String, String> mockedEnv = new HashMap<>();

    @Before
    public void setUp() {
        new MockUp<System>() {
            @Mock
            public String getenv(final String key) {
                mockedEnv.putAll(System.getenv());
                return mockedEnv.get(key);
            }
        };
    }

    @Test
    public void should_find_property_by_sequence() {
        // should find property from test resource
        Resource resource = propertyResourceLoader.find();
        Assert.assertEquals(ClassPathResource.class, resource.getClass());

        // should find property from system property
        System.setProperty("config.path", "/");
        resource = propertyResourceLoader.find();
        Assert.assertEquals(FileSystemResource.class, resource.getClass());
        Assert.assertEquals(Paths.get("/").toString(), ((FileSystemResource) resource).getPath());

        // should find property from env
        mockedEnv.put("FLOW_CONFIG_PATH", "/bin");
        resource = propertyResourceLoader.find();
        Assert.assertEquals(FileSystemResource.class, resource.getClass());
        Assert.assertEquals(Paths.get("/bin").toString(), ((FileSystemResource) resource).getPath());
    }
}
