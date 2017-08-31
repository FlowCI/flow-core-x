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

import com.flow.platform.api.dao.NodeResultDao;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeResultKey;
import com.flow.platform.api.domain.job.NodeTag;
import com.flow.platform.api.domain.node.Step;
import com.flow.platform.api.service.node.NodeService;
import com.flow.platform.api.util.EnvUtil;
import com.flow.platform.api.util.NodeUtil;
import com.flow.platform.core.exception.IllegalStatusException;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.util.Logger;
import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author gyfirim
 */
@Service
@Transactional
public class NodeResultServiceImpl implements NodeResultService {

    private final static Logger LOGGER = new Logger(NodeResultService.class);

    @Autowired
    private NodeResultDao nodeResultDao;

    @Autowired
    private NodeService nodeService;

    @Override
    public NodeResult create(Job job) {
        String nodePath = job.getNodePath();
        Node root = nodeService.find(nodePath);

        if (root == null) {
            throw new IllegalStatusException("Job related node is empty, please check");
        }

        final NodeResult[] rootResult = {null};

        // save all empty node result to db
        NodeUtil.recurse(root, node -> {
            NodeResult nodeResult = new NodeResult(job.getId(), node.getPath());
            nodeResult.setName(node.getName());
            nodeResult.setNodeTag(node instanceof Flow ? NodeTag.FLOW : NodeTag.STEP);
            nodeResult.setOutputs(node.getEnvs());
            nodeResultDao.save(nodeResult);

            if (node.equals(root)) {
                rootResult[0] = nodeResult;
            }
        });

        return rootResult[0];
    }

    @Override
    public NodeResult find(String path, BigInteger jobID) {
        return nodeResultDao.get(new NodeResultKey(jobID, path));
    }

    @Override
    public List<NodeResult> list(Job job) {
        return nodeResultDao.list(job.getId());
    }

    @Override
    public void save(NodeResult result) {
        nodeResultDao.update(result);
    }

    @Override
    public NodeResult update(Job job, Node node, Cmd cmd) {
        NodeResult currentResult = find(node.getPath(), job.getId());
        updateCurrent(currentResult, cmd);

        updateParent(job, node);
        return currentResult;
    }

    private void updateCurrent(NodeResult currentResult, Cmd cmd) {
        NodeStatus newStatus = NodeStatus.transfer(cmd);

        // keep job step status sorted
        if (currentResult.getStatus().getLevel() >= newStatus.getLevel()) {
            return;
        }

        currentResult.setStatus(newStatus);
        currentResult.setLogPaths(cmd.getLogPaths());

        CmdResult cmdResult = cmd.getCmdResult();
        if (cmdResult != null) {
            currentResult.setDuration(cmdResult.getTotalDuration());
            currentResult.setExitCode(cmdResult.getExitValue());
            currentResult.setStartTime(cmdResult.getStartTime());
            currentResult.setFinishTime(cmdResult.getFinishTime());
            currentResult.setOutputs(cmdResult.getOutput());
        }

        nodeResultDao.update(currentResult);
    }

    private void updateParent(Job job, Node current) {
        Node parent = current.getParent();
        if (parent == null) {
            return;
        }

        // get related node result
        Node first = (Node) parent.getChildren().get(0);
        NodeResult currentResult = find(current.getPath(), job.getId());
        NodeResult firstResult = find(first.getPath(), job.getId());
        NodeResult parentResult = find(parent.getPath(), job.getId());

        // update parent node result data
        EnvUtil.merge(currentResult.getOutputs(), parentResult.getOutputs(), true);
        parentResult.setStartTime(firstResult.getStartTime());
        parentResult.setFinishTime(currentResult.getFinishTime());
        parentResult.setExitCode(currentResult.getExitCode());
        try {
            long duration = Duration.between(currentResult.getStartTime(), currentResult.getFinishTime()).getSeconds();
            parentResult.setDuration(parentResult.getDuration() + duration);
        } catch (Throwable e) {
            parentResult.setDuration(0L); // cache exception if start or finish time is null
        }

        if (shouldUpdateParentStatus(current, currentResult)) {
            parentResult.setStatus(currentResult.getStatus());
        }

        nodeResultDao.update(parentResult);
        LOGGER.debug("Update parent '%s' status to '%s' on job '%s'",
            parentResult.getPath(),
            parentResult.getStatus(),
            job.getId()
        );

        // recursive bottom up to update parent node result
        updateParent(job, parent);
    }

    private static boolean shouldUpdateParentStatus(Node current, NodeResult result) {
        // update parent status if current on running and it is the first one in the tree level
        if (result.isRunning()) {
            if (current.getPrev() == null) {
                return true;
            }
        }

        // update parent status if current node on step status
        if (result.isStop()) {
            return true;
        }

        // update parent status if current on success and it is the last one in the tree level
        if (result.isSucess()) {
            if (current.getNext() == null) {
                return true;
            }
        }

        // update parent status if current on failure and it is not allow failure
        if (result.isFailure()) {
            if (current instanceof Step) {
                Step step = (Step) current;
                if (!step.getAllowFailure()) {
                    return true;
                }
            }
        }

        return false;
    }
}
