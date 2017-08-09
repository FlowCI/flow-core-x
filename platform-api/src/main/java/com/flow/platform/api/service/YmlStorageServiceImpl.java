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

package com.flow.platform.api.service;

import com.flow.platform.api.dao.YmlStorageDao;
import com.flow.platform.api.domain.YmlStorage;
import com.flow.platform.api.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */


@Service(value = "ymlStorageService")
public class YmlStorageServiceImpl implements  YmlStorageService{

    @Autowired
    private YmlStorageDao ymlStorageDao;

    @Override
    public void save(String nodePath, String yml) {
        YmlStorage storage = ymlStorageDao.get(nodePath);
        if(storage == null){
            storage = new YmlStorage(nodePath, yml);
            ymlStorageDao.save(storage);
        }else{
            storage.setFile(yml);
            ymlStorageDao.update(storage);
        }
    }

    @Override
    public YmlStorage get(String nodePath) {
        YmlStorage storage = ymlStorageDao.get(nodePath);
        if(storage == null){
            throw new NotFoundException("yml storage not found");
        }
        return storage;
    }
}
