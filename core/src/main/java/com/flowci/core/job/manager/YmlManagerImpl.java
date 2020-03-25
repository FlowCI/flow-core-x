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

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.job.dao.JobYmlDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobYml;
import com.flowci.exception.NotFoundException;
import com.flowci.tree.FlowNode;
import com.flowci.tree.Node;
import com.flowci.tree.NodeTree;
import com.flowci.tree.YmlParser;
import com.github.benmanes.caffeine.cache.Cache;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Log4j2
@Component
public class YmlManagerImpl implements YmlManager {

    @Autowired
    private Cache<String, NodeTree> jobTreeCache;

    @Autowired
    private JobYmlDao jobYmlDao;

    @Override
    public JobYml get(Job job) {
        Optional<JobYml> optional = jobYmlDao.findById(job.getId());

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new NotFoundException("The yml for job {0} is not existed", job.getId());
    }

    @Override
    public JobYml create(Flow flow, Job job, String yml) {
        JobYml jobYml = new JobYml(job.getId(), flow.getName(), yml);
        return jobYmlDao.insert(jobYml);
    }

    @Override
    public NodeTree getTree(Job job) {
        return jobTreeCache.get(job.getId(), s -> {
            log.debug("Cache tree for job: {}", job.getId());
            JobYml yml = jobYmlDao.findById(job.getId()).get();
            FlowNode root = YmlParser.load(yml.getName(), yml.getRaw());
            return NodeTree.create(root);
        });
    }
}
