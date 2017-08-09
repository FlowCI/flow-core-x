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
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.exception.NotFoundException;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.util.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author lhl
 */

@Service(value = "jobYmlStorageService")
public class JobYmlStorageServiceImpl implements JobYmlStorageService {

    private final Logger LOGGER = new Logger(JobYmlStorage.class);

    @Autowired
    private JobYmlStorageDao jobYmlStorageDao;

    // 1 day expire
    private Cache<BigInteger, Map<String, Node>> nodeCache = CacheBuilder.newBuilder()
        .expireAfterAccess(3600 * 24, TimeUnit.SECONDS).maximumSize(1000).build();

    private Map<String, Node> get(final BigInteger jobId) {
        Map<String, Node> map = null;
        try {
            map = nodeCache.get(jobId, () -> getNodeMapFromYam(jobId));
        } catch (ExecutionException e) {
            LOGGER.trace(String.format("get yaml from jobYamlService error - %s", e));
        }
        return map;
    }

    private Map<String, Node> getNodeMapFromYam(BigInteger jobId) {
        JobYmlStorage jobYmlStorage = jobYmlStorageDao.get(jobId);
        if (jobYmlStorage == null) {
            throw new NotFoundException(String.format("jobYmlStorage not found, jobId is %s", jobId));
        }

        Map<String, Node> nodeMap = new HashMap<>();
        Node root = NodeUtil.buildFromYml(jobYmlStorage.getFile());
        NodeUtil.recurse(root, item -> nodeMap.put(item.getPath(), item));
        return nodeMap;
    }


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
    }

    @Override
    public Node get(BigInteger jobId, String path) {
        Map<String, Node> nodeMap = get(jobId);

        return nodeMap.get(path);
    }

}
