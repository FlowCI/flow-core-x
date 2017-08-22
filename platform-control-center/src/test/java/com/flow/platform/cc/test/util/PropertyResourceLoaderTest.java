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

import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import com.flow.platform.cc.resource.PropertyResourceLoader;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * @author yang
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertyResourceLoader.class})
@Ignore
public class PropertyResourceLoaderTest {

    private PropertyResourceLoader propertyResourceLoader = new PropertyResourceLoader();

    @Before
    public void setUp() {
        mockStatic(System.class);
    }

    @Test
    public void should_find_property_by_sequence() {
        // should find property from test resource
        Resource resource = propertyResourceLoader.find();
        Assert.assertEquals(ClassPathResource.class, resource.getClass());

        // should find property from system property
        when(System.getProperty("flow.cc.config.path")).thenReturn("/");

        resource = propertyResourceLoader.find();
        Assert.assertEquals(FileSystemResource.class, resource.getClass());
        Assert.assertEquals(Paths.get("/").toString(), ((FileSystemResource) resource).getPath());

        // should find property from env
        when(System.getenv("FLOW_CC_CONFIG_PATH")).thenReturn("/bin");

        resource = propertyResourceLoader.find();
        Assert.assertEquals(FileSystemResource.class, resource.getClass());
        Assert.assertEquals(Paths.get("/bin").toString(), ((FileSystemResource) resource).getPath());
    }
}
