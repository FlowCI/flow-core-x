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
import com.flow.platform.api.dao.YmlDao;
import com.flow.platform.api.domain.node.Yml;
import com.flow.platform.api.test.TestBase;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class YmlDaoTest extends TestBase {

    @Test
    public void should_save_and_get_success() {
        Yml ymlStorage = new Yml("/flow", "YML config");
        ymlDao.save(ymlStorage);

        Yml ymlStorage1 = ymlDao.get(ymlStorage.getNodePath());
        Assert.assertNotNull(ymlStorage1);
        Assert.assertEquals("YML config", ymlStorage1.getFile());

        ymlStorage.setFile(null);
        ymlDao.update(ymlStorage);
        ymlStorage1 = ymlDao.get(ymlStorage.getNodePath());
        Assert.assertEquals(null, ymlStorage1.getFile());

        ymlStorage.setFile("");
        ymlDao.update(ymlStorage);
        ymlStorage1 = ymlDao.get(ymlStorage.getNodePath());
        Assert.assertEquals("", ymlStorage1.getFile());
    }

    @Test
    public void should_save_and_get_yml_success() throws IOException {
        ClassLoader classLoader = YmlDaoTest.class.getClassLoader();
        URL resource = classLoader.getResource("flow.yaml");
        File path = new File(resource.getFile());
        String ymlString = Files.toString(path, AppConfig.DEFAULT_CHARSET);
        Yml storage = new Yml("/flow", ymlString);
        ymlDao.save(storage);

        Yml storage1 = ymlDao.get(storage.getNodePath());
        Assert.assertNotNull(storage1);
        Assert.assertEquals(ymlString, storage1.getFile());
    }

    @Test
    public void should_update_success() {
        Yml storage = new Yml("/flow", "Yml Body");
        ymlDao.save(storage);
        Yml ymlStorage = ymlDao.get(storage.getNodePath());
        Assert.assertNotNull(ymlStorage);

        storage.setFile("Yml");
        ymlDao.update(storage);

        ymlStorage = ymlDao.get(storage.getNodePath());
        Assert.assertEquals(storage.getFile(), ymlStorage.getFile());
    }

    @Test
    public void should_delete_success() {
        String path = "/flow";

        Yml storage = new Yml(path, "Yml Body");
        ymlDao.save(storage);
        Assert.assertNotNull(ymlDao.get(path));

        ymlDao.delete(new Yml(path, null));
        Assert.assertEquals(null, ymlDao.get(storage.getNodePath()));
    }
}
