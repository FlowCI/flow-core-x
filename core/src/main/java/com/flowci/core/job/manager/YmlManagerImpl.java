/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.job.manager;

import com.flowci.core.job.dao.JobYmlDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobYml;
import com.flowci.exception.NotFoundException;
import com.flowci.tree.FlowNode;
import com.flowci.tree.NodeTree;
import com.flowci.tree.YmlParser;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author yang
 */
@Slf4j
@Component
@AllArgsConstructor
public class YmlManagerImpl implements YmlManager {

    private final Cache<String, NodeTree> jobTreeCache;

    private final JobYmlDao jobYmlDao;

    @Override
    public FlowNode parse(JobYml jobYml) {
        return YmlParser.load(jobYml.getRawArray());
    }

    @Override
    public FlowNode parse(String... raw) {
        return YmlParser.load(raw);
    }

    @Override
    public JobYml get(Job job) {
        Optional<JobYml> optional = jobYmlDao.findById(job.getId());

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new NotFoundException("The yml for job {0} is not existed", job.getId());
    }

    @Override
    public JobYml create(JobYml jobYml) {
        return jobYmlDao.save(jobYml);
    }

    @Override
    public NodeTree getTree(Job job) {
        return getTree(job.getId());
    }

    @Override
    public NodeTree getTree(String jobId) {
        return jobTreeCache.get(jobId, s -> {
            log.debug("Cache tree for job: {}", jobId);
            JobYml yml = jobYmlDao.findById(jobId).get();
            FlowNode root = YmlParser.load(yml.getRawArray());
            return NodeTree.create(root);
        });
    }

    @Override
    public void delete(Job job) {
        jobYmlDao.deleteById(job.getId());
    }
}
