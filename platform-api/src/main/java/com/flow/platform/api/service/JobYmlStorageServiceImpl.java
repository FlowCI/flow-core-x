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

import static com.google.common.collect.Maps.newHashMap;

import com.flow.platform.api.dao.JobYmlStorageDao;
import com.flow.platform.api.domain.JobYmlStorage;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.exception.NotFoundException;
import com.flow.platform.api.util.NodeUtil;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author lhl
 */

@Service(value = "jobYmlStorageService")
public class JobYmlStorageServiceImpl implements JobYmlStorageService {

    @Autowired
    private JobYmlStorageDao jobYmlStorageDao;

    private Map<BigInteger, Map<String, Node>> mocNodes = newHashMap();

    @Override
    public void save(BigInteger jobId, String yml) {
        JobYmlStorage jobYmlStorage = jobYmlStorageDao.get(jobId);
        if (jobYmlStorage == null) {
            jobYmlStorage = new JobYmlStorage(jobId, yml);
            jobYmlStorageDao.save(jobYmlStorage);
        } else {
            jobYmlStorage.setFile(yml);
            jobYmlStorageDao.update(jobYmlStorage);
        }

        addNodeToCache(jobYmlStorage);
    }

    @Override
    public Node get(BigInteger jobId, String path) {
        Map<String, Node> nodeMap = mocNodes.get(jobId);
        if (nodeMap == null) {

            //load to cache
            JobYmlStorage jobYmlStorage = jobYmlStorageDao.get(jobId);
            if (jobYmlStorage == null) {
                throw new NotFoundException(String.format("jobYmlStorage not found, jobId is %s", jobId));
            }

            addNodeToCache(jobYmlStorage);
            nodeMap = mocNodes.get(jobId);
        }
        
        return nodeMap.get(path);
    }

    private Node addNodeToCache(JobYmlStorage jobYmlStorage) {
        Node root = NodeUtil.buildFromYml(jobYmlStorage.getFile());
        NodeUtil.recurse(root, item -> {

            Map<String, Node> nodeMap = mocNodes.get(jobYmlStorage.getJobId());
            if (nodeMap == null) {
                nodeMap = new HashMap<>();
            }
            nodeMap.put(item.getPath(), item);
            mocNodes.put(jobYmlStorage.getJobId(), nodeMap);
        });
        return root;
    }
}
