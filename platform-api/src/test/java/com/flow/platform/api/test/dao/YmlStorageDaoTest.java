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

import com.flow.platform.api.dao.YmlStorageDao;
import com.flow.platform.api.domain.YmlStorage;
import com.flow.platform.api.test.TestBase;
import java.nio.charset.Charset;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class YmlStorageDaoTest extends TestBase{

    @Autowired
    private YmlStorageDao ymlStorageDao;

    @Test
    public void should_save_and_get_success(){
        YmlStorage ymlStorage = new YmlStorage();
        ymlStorage.setFile("sssss");
        ymlStorage.setNodePath("/flow");
        ymlStorageDao.save(ymlStorage);

        YmlStorage ymlStorage1 = ymlStorageDao.get(ymlStorage.getNodePath());
        Assert.assertNotNull(ymlStorage1);
        Assert.assertEquals("sssss", ymlStorage1.getFile());
    }
}
