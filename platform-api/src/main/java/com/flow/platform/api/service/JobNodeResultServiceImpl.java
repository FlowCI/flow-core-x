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

import com.flow.platform.api.dao.NodeResultDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.Node;
import com.flow.platform.api.domain.NodeResult;
import com.flow.platform.api.domain.NodeResultKey;
import com.flow.platform.api.domain.NodeTag;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.core.exception.IllegalStatusException;
import java.math.BigInteger;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author gyfirim
 */
@Service
@Transactional
public class JobNodeResultServiceImpl implements JobNodeResultService {

    @Autowired
    private NodeResultDao nodeResultDao;

    @Autowired
    private NodeService nodeService;

    @Override
    public void create(Job job) {
        String nodePath = job.getNodePath();
        Node root = nodeService.find(nodePath);

        if (root == null) {
            throw new IllegalStatusException("Job related node is empty, please check");
        }

        // save all empty node result to db
        NodeUtil.recurse(root, node -> {
            NodeResult nodeResult = new NodeResult(job.getId(), node.getPath());
            nodeResult.setName(node.getName());
            nodeResult.setNodeTag(node instanceof Flow ? NodeTag.FLOW : NodeTag.STEP);
            nodeResultDao.save(nodeResult);
        });
    }

    @Override
    public NodeResult find(String path, BigInteger jobID) {
        return nodeResultDao.get(new NodeResultKey(jobID, path));
    }

    @Override
    public NodeResult update(NodeResult nodeResult) {
        nodeResultDao.update(nodeResult);
        return nodeResult;
    }

    @Override
    public List<NodeResult> list(Job job) {
        return nodeResultDao.list(job);
    }
}
