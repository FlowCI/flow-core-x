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
package com.flow.platform.api.service.job;

import com.flow.platform.api.dao.job.JobYmlDao;
import com.flow.platform.api.domain.job.JobYml;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.util.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lhl
 */

@Service
@Transactional
public class JobNodeServiceImpl implements JobNodeService {

    private final Logger LOGGER = new Logger(JobYml.class);

    @Autowired
    private JobYmlDao jobYmlDao;

    // 1 day expire
    private Cache<BigInteger, NodeTree> nodeCache = CacheBuilder
        .newBuilder()
        .expireAfterAccess(3600 * 24, TimeUnit.SECONDS)
        .maximumSize(1000).build();

    @Override
    public void save(final BigInteger jobId, final String yml) {
        JobYml jobYmlStorage = new JobYml(jobId, yml);
        jobYmlDao.saveOrUpdate(jobYmlStorage);
        nodeCache.invalidate(jobId);
    }

    @Override
    public NodeTree get(final BigInteger jobId) {
        try {
            return nodeCache.get(jobId, () -> {
                JobYml jobYmlStorage = jobYmlDao.get(jobId);
                if (jobYmlStorage == null) {
                    throw new NotFoundException(String.format("Job node of job '%s' not found", jobId));
                }

                return new NodeTree(jobYmlStorage.getFile());
            });
        } catch (ExecutionException e) {
            LOGGER.warn(String.format("get yaml from jobYamlService error - %s", e));
            return null;
        }
    }
}
