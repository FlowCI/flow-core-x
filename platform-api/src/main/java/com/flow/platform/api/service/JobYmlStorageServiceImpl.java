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

import com.flow.platform.api.dao.JobYmlStorageDao;
import com.flow.platform.api.domain.JobYmlStorage;
import com.flow.platform.api.exception.NotFoundException;
import com.flow.platform.api.util.NodeUtil;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import javax.xml.soap.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author lhl
 */

@Service(value = "jobYmlStorageService")
public class JobYmlStorageServiceImpl implements JobYmlStorageService {

    @Autowired
    private JobYmlStorageDao jobYmlStorgeDao;

    private  Map<BigInteger, Map<String, Node>> mocNodes = new HashMap<>();

    @Override
    public void save(BigInteger jobId, String yml){
        JobYmlStorage jobYmlStorage = jobYmlStorgeDao.get(jobId);
        if (jobYmlStorage == null){
            jobYmlStorage = new JobYmlStorage(jobId, yml);
            jobYmlStorgeDao.save(jobYmlStorage);
        } else {
            jobYmlStorage.setFile(yml);
            jobYmlStorgeDao.update(jobYmlStorage);
        }
    }

    @Override
    public Node get(BigInteger jobId){
        JobYmlStorage jobYmlStorage = jobYmlStorgeDao.get(jobId);
        if(jobYmlStorage == null) {
            throw new NotFoundException("job yml storage not found");
        }
        Node node = (Node) NodeUtil.buildFromYml(jobYmlStorage.getFile());
        NodeUtil.recurse((com.flow.platform.api.domain.Node) node, item->{
            mocNodes.get(jobId).put(item.getPath(), (Node) item);
        });
        return node;
    }


    @Override
    public Node get(BigInteger jobId, String path) {
        return mocNodes.get(jobId).get(path);
    }
}
