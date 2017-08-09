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
import java.math.BigInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author gyfirim
 */
@Service(value = "nodeResultService")
public class NodeResultServiceImpl implements NodeResultService {

    @Autowired
    private NodeResultDao nodeResultDao;

    @Autowired
    private NodeService nodeService;

    @Override
    public NodeResult create(Job job) {
        String nodePath = job.getNodePath();
        Node root = nodeService.find(nodePath);

        // save root to db
        NodeResult jobNodeRoot = save(job.getId(), root);

        // save children nodes to db
        NodeUtil.recurse(root, item -> {
            if (root != item) {
                save(job.getId(), item);
            }
        });

        return jobNodeRoot;
    }

    @Override
    public NodeResult find(String path, BigInteger jobID) {
        NodeResultKey nodeResultKey = new NodeResultKey(jobID, path);
        return nodeResultDao.get(nodeResultKey);
    }

    @Override
    public NodeResult save(BigInteger jobId, Node node) {
        NodeResult nodeResult = new NodeResult(jobId, node.getPath());
        nodeResult.setName(node.getName());
        nodeResult.setNodeTag(node instanceof Flow ? NodeTag.FLOW : NodeTag.STEP);
        return nodeResultDao.save(nodeResult);
    }

    @Override
    public NodeResult update(NodeResult nodeResult) {
        nodeResultDao.update(nodeResult);
        return nodeResult;
    }
}
