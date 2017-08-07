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

import com.flow.platform.api.dao.JobNodeDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.JobNode;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.NodeTag;
import com.flow.platform.api.domain.YmlStorage;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.util.Logger;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */
@Service(value = "jobNodeService")
public class JobNodeServiceImpl implements JobNodeService {

    private final Map<String, JobNode> mocNodeList = new HashMap<>();
    private final Logger LOGGER = new Logger(JobService.class);

    @Autowired
    private NodeService nodeService;

    @Autowired
    private JobNodeDao jobNodeDao;

    @Autowired
    private YmlStorageService ymlStorageService;

    @Autowired
    private JobYmlStorgeService jobYmlStorgeService;

    @Autowired
    private JobService jobService;

    @Override
    public JobNode create(Job job) {
        String nodePath = job.getNodePath();
        YmlStorage storage = ymlStorageService.get(nodePath);

        Node root = NodeUtil.buildFromYml(storage.getFile());

        // save root to db
        JobNode jobNodeRoot = save(job.getId(), root);

        // save children nodes to db
        NodeUtil.recurse(root, item -> {
            if (root != item){
                JobNode jobNode = save(job.getId(), item);
                mocNodeList.put(root.getPath(), jobNode);
            }
        });
        return jobNodeRoot;
    }


    @Override
    public JobNode update(JobNode jobNode) {
        jobNodeDao.update(jobNode);
        return jobNode;
    }

    @Override
    public JobNode find(String nodePath, BigInteger jobId) {
        JobNode jobNode = mocNodeList.get(nodePath);
        if(jobNode == null){
            Job job  = jobService.find(jobId);
            if (job == null){
                Job job1 = jobService.createJob(nodePath);
                create(job1);
                return find(nodePath, job1.getId());
            } else {
                create(job);
                return find(nodePath, jobId);
            }
        } else {
          return jobNode;
        }
    }

   @Override
    public JobNode save(BigInteger id, Node item) {
        JobNode node = new JobNode(id, item.getPath());
        node.setNodeTag(item instanceof Flow ? NodeTag.FLOW : NodeTag.STEP);
        return jobNodeDao.save(node);
    }
}