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

package com.flowci.core.job.service;

import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.dao.ExecutedCmdDao;
import com.flowci.core.job.domain.Executed;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Step;
import com.flowci.core.job.event.StepUpdateEvent;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.exception.NotFoundException;
import com.flowci.tree.NodePath;
import com.flowci.tree.NodeTree;
import com.flowci.tree.RegularStepNode;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * ExecutedCmd == Step
 *
 * @author yang
 */
@Log4j2
@Service
public class StepServiceImpl implements StepService {

    @Autowired
    private Cache<String, List<Step>> jobStepCache;

    @Autowired
    private ExecutedCmdDao executedCmdDao;

    @Autowired
    private YmlManager ymlManager;

    @Autowired
    private SpringEventManager eventManager;

    @Override
    public void init(Job job) {
        NodeTree tree = ymlManager.getTree(job);
        List<Step> steps = new LinkedList<>();

        for (StepNode step : tree.getSteps()) {
            if (step instanceof RegularStepNode) {
                RegularStepNode rStep = (RegularStepNode) step;
                Step cmd = newInstance(job, rStep);
                steps.add(cmd);
            }
        }

        executedCmdDao.insert(steps);
        eventManager.publish(new StepUpdateEvent(this, job.getId(), steps, true));
    }

    @Override
    public Step get(String jobId, String nodePath) {
        Optional<Step> optional = executedCmdDao.findByJobIdAndNodePath(jobId, nodePath);

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new NotFoundException("Executed cmd for job {0} - {1} not found", jobId, nodePath);
    }

    @Override
    public Step get(String id) {
        Optional<Step> optional = executedCmdDao.findById(id);

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new NotFoundException("Executed cmd {0} not found", id);
    }

    @Override
    public List<Step> list(Job job) {
        return list(job.getId(), job.getFlowId(), job.getBuildNumber());
    }

    @Override
    public String toVarString(Job job, StepNode current) {
        StringBuilder builder = new StringBuilder();
        for (Step step : list(job)) {
            NodePath path = NodePath.create(step.getNodePath());
            builder.append(path.name())
                    .append("=")
                    .append(step.getStatus().name());
            builder.append(";");

            if (current != null) {
                if (step.getNodePath().equals(current.getPath().getPathInStr())) {
                    break;
                }
            }
        }
        return builder.deleteCharAt(builder.length() - 1).toString();
    }

    @Override
    public Step toStatus(String jobId, String nodePath, Step.Status status, String err) {
        Step entity = get(jobId, nodePath);
        return toStatus(entity, status, err);
    }

    @Override
    public Step toStatus(Step entity, Executed.Status status, String err) {
        if (entity.getStatus() == status) {
            return entity;
        }

        entity.setStatus(status);
        entity.setError(err);
        executedCmdDao.save(entity);

        // update parent status
        NodePath parentPath = NodePath.create(entity.getParent());
        if (!entity.isRootStep()) {
            Step parentStep = get(entity.getJobId(), parentPath.getPathInStr());
            parentStep.setStatus(entity.getStatus());
            executedCmdDao.save(parentStep);
        }

        String jobId = entity.getJobId();
        jobStepCache.invalidate(jobId);

        List<Step> steps = list(jobId, entity.getFlowId(), entity.getBuildNumber());
        eventManager.publish(new StepUpdateEvent(this, jobId, steps, false));
        return entity;
    }

    @Override
    public void resultUpdate(Step cmd) {
        checkNotNull(cmd.getId());
        Step entity = get(cmd.getId());

        // only update properties should from agent
        entity.setProcessId(cmd.getProcessId());
        entity.setCode(cmd.getCode());
        entity.setStartAt(cmd.getStartAt());
        entity.setFinishAt(cmd.getFinishAt());
        entity.setLogSize(cmd.getLogSize());
        entity.setOutput(cmd.getOutput());

        // change status and save
        toStatus(entity, cmd.getStatus(), cmd.getError());
    }

    @Override
    public Long delete(Flow flow) {
        return executedCmdDao.deleteByFlowId(flow.getId());
    }

    @Override
    public Long delete(Job job) {
        return executedCmdDao.deleteByJobId(job.getId());
    }

    private List<Step> list(String jobId, String flowId, long buildNumber) {
        return jobStepCache.get(jobId,
                s -> executedCmdDao.findByFlowIdAndBuildNumber(flowId, buildNumber));
    }

    private static Step newInstance(Job job, RegularStepNode node) {
        String cmdId = job.getId() + node.getPathAsString();

        return new Step()
                .setId(Base64.getEncoder().encodeToString(cmdId.getBytes()))
                .setFlowId(job.getFlowId())
                .setBuildNumber(job.getBuildNumber())
                .setJobId(job.getId())
                .setNodePath(node.getPathAsString())
                .setAllowFailure(node.isAllowFailure())
                .setPlugin(node.getPlugin())
                .setDockers(node.getDockers())
                .setRootStep(node.isRootStep())
                .setParent(node.getParent().getPathAsString())
                .setChildren(childrenPaths(node));
    }

    private static List<String> childrenPaths(RegularStepNode node) {
        if (!node.hasChildren()) {
            return null;
        }

        List<String> paths = new ArrayList<>(node.getChildren().size());
        for (StepNode child : node.getChildren()) {
            paths.add(child.getPath().getPathInStr());
        }
        return paths;
    }
}
