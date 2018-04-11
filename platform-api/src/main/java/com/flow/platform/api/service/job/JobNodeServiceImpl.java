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
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobYml;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.node.NodeTree;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.core.exception.NotFoundException;
import com.flow.platform.util.Logger;
import java.math.BigInteger;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * @author lhl
 */
@Log4j2
@Service
public class JobNodeServiceImpl implements JobNodeService {

    @Autowired
    private JobYmlDao jobYmlDao;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private CacheManager cacheManager;

    @Override
    public void save(final Job job, final String yml) {
        JobYml jobYmlStorage = new JobYml(job.getId(), yml);
        jobYmlDao.saveOrUpdate(jobYmlStorage);
        jobNodeCache().evict(job.getId());
    }

    @Override
    public NodeTree get(final Job job) {
        final Node flow = nodeService.find(job.getNodePath()).root();

        NodeTree tree = jobNodeCache().get(job.getId(), () -> {
            JobYml jobYml = find(job);
            if (jobYml == null) {
                return null;
            }
            return new NodeTree(jobYml.getFile(), flow);
        });

        // cleanup cache if null value
        if (tree == null) {
            jobNodeCache().evict(job.getId());
            return null;
        }

        return tree;
    }

    @Override
    public JobYml find(Job job) {
        JobYml jobYml = jobYmlDao.get(job.getId());
        if (jobYml == null) {
            throw new NotFoundException(String.format("Job node of job '%s' not found", job.getId()));
        }

        return jobYml;
    }

    @Override
    public void delete(List<BigInteger> jobIds) {
        jobYmlDao.delete(jobIds);
    }

    private Cache jobNodeCache() {
        return cacheManager.getCache("jobNodeTreeCache");
    }
}
