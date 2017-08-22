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

package com.flow.platform.api.test.dao;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.dao.YmlStorageDao;
import com.flow.platform.api.domain.YmlStorage;
import com.flow.platform.api.test.TestBase;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class YmlStorageDaoTest extends TestBase {

    @Autowired
    private YmlStorageDao ymlStorageDao;

    @Test
    public void should_save_and_get_success() {
        YmlStorage ymlStorage = new YmlStorage("/flow", "YML config");
        ymlStorageDao.save(ymlStorage);

        YmlStorage ymlStorage1 = ymlStorageDao.get(ymlStorage.getNodePath());
        Assert.assertNotNull(ymlStorage1);
        Assert.assertEquals("YML config", ymlStorage1.getFile());

        ymlStorage.setFile(null);
        ymlStorageDao.update(ymlStorage);
        ymlStorage1 = ymlStorageDao.get(ymlStorage.getNodePath());
        Assert.assertEquals(null, ymlStorage1.getFile());

        ymlStorage.setFile("");
        ymlStorageDao.update(ymlStorage);
        ymlStorage1 = ymlStorageDao.get(ymlStorage.getNodePath());
        Assert.assertEquals("", ymlStorage1.getFile());
    }

    @Test
    public void should_save_and_get_yml_success() throws IOException {
        ClassLoader classLoader = YmlStorageDaoTest.class.getClassLoader();
        URL resource = classLoader.getResource("flow.yaml");
        File path = new File(resource.getFile());
        String ymlString = Files.toString(path, AppConfig.DEFAULT_CHARSET);
        YmlStorage storage = new YmlStorage("/flow", ymlString);
        ymlStorageDao.save(storage);

        YmlStorage storage1 = ymlStorageDao.get(storage.getNodePath());
        Assert.assertNotNull(storage1);
        Assert.assertEquals(ymlString, storage1.getFile());
    }

    @Test
    public void should_update_success() {
        YmlStorage storage = new YmlStorage("/flow", "Yml Body");
        ymlStorageDao.save(storage);
        YmlStorage ymlStorage = ymlStorageDao.get(storage.getNodePath());
        Assert.assertNotNull(ymlStorage);

        storage.setFile("Yml");
        ymlStorageDao.update(storage);

        ymlStorage = ymlStorageDao.get(storage.getNodePath());
        Assert.assertEquals(storage.getFile(), ymlStorage.getFile());
    }

    @Test
    public void should_delete_success() {
        String path = "/flow";

        YmlStorage storage = new YmlStorage(path, "Yml Body");
        ymlStorageDao.save(storage);
        Assert.assertNotNull(ymlStorageDao.get(path));

        ymlStorageDao.delete(new YmlStorage(path, null));
        Assert.assertEquals(null, ymlStorageDao.get(storage.getNodePath()));
    }
}
