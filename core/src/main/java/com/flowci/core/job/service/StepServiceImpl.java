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
import com.flowci.tree.*;
import com.flowci.util.StringHelper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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

        tree.getFlatted().forEach((path, node) -> {
            Step cmd = newInstance(job, node);
            steps.add(cmd);
        });

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
    public List<Step> list(Job job, Collection<Executed.Status> status) {
        return executedCmdDao.findByJobIdAndStatusIn(job.getId(), status);
    }

    @Override
    public String toVarString(Job job, Step current) {
        StringBuilder builder = new StringBuilder();
        for (Step step : list(job)) {
            NodePath path = NodePath.create(step.getNodePath());
            builder.append(path.name())
                    .append("=")
                    .append(step.getStatus().name());
            builder.append(";");

            if (step.getNodePath().equals(current.getNodePath())) {
                break;
            }
        }
        return builder.deleteCharAt(builder.length() - 1).toString();
    }

    @Override
    public Collection<Step> toStatus(Collection<Step> steps, Executed.Status status, String err) {
        String jobId = "";
        String flowId = "";
        long buildNumber = 0L;

        for (Step step : steps) {
            jobId = step.getJobId();
            flowId = step.getFlowId();
            buildNumber = step.getBuildNumber();

            step.setStatus(status);
            step.setError(err);
        }

        if (steps.isEmpty()) {
            return steps;
        }

        executedCmdDao.saveAll(steps);

        jobStepCache.invalidate(jobId);
        List<Step> list = list(jobId, flowId, buildNumber);
        eventManager.publish(new StepUpdateEvent(this, jobId, list, false));
        return steps;
    }

    @Override
    public Step toStatus(Step entity, Executed.Status status, String err, boolean allChildren) {
        saveStatus(entity, status, err);

        // update parent status if not post step
        if (!entity.isPost()) {
            Step parent = getWithNullReturn(entity.getJobId(), entity.getParent());
            updateAllParents(parent, entity);
        }

        if (allChildren) {
            NodeTree tree = ymlManager.getTree(entity.getJobId());
            NodePath path = NodePath.create(entity.getNodePath());
            Node node = tree.get(path);

            for (Node child : node.getChildren()) {
                Step childStep = get(entity.getJobId(), child.getPathAsString());
                childStep.setStartAt(entity.getStartAt());
                childStep.setFinishAt(entity.getFinishAt());
                saveStatus(childStep, status, err);
            }
        }

        String jobId = entity.getJobId();
        jobStepCache.invalidate(jobId);

        List<Step> steps = list(jobId, entity.getFlowId(), entity.getBuildNumber());
        eventManager.publish(new StepUpdateEvent(this, jobId, steps, false));
        return entity;
    }

    @Override
    public void resultUpdate(Step stepFromAgent) {
        // change status and save
        toStatus(stepFromAgent, stepFromAgent.getStatus(), stepFromAgent.getError(), false);
    }

    @Override
    public Long delete(Flow flow) {
        return executedCmdDao.deleteByFlowId(flow.getId());
    }

    @Override
    public Long delete(Job job) {
        return executedCmdDao.deleteByJobId(job.getId());
    }

    private Step getWithNullReturn(String jobId, String nodePath) {
        if (nodePath == null) {
            return null;
        }

        Optional<Step> optional = executedCmdDao.findByJobIdAndNodePath(jobId, nodePath);
        return optional.orElse(null);
    }

    private void updateAllParents(Step parent, Step current) {
        if (parent == null || parent.isRoot()) {
            return;
        }

        parent.setStatus(current.getStatus());
        parent.setError(current.getError());
        parent.setFinishAt(current.getFinishAt());

        if (parent.getStartAt() == null) {
            parent.setStartAt(current.getStartAt());
        }

        executedCmdDao.save(parent);

        Step p = getWithNullReturn(parent.getJobId(), parent.getParent());
        updateAllParents(p, current);
    }

    private Step saveStatus(Step step, Step.Status status, String error) {
        step.setError(error);
        step.setStatus(status);
        executedCmdDao.save(step);
        return step;
    }

    private List<Step> list(String jobId, String flowId, long buildNumber) {
        return jobStepCache.get(jobId,
                s -> executedCmdDao.findByFlowIdAndBuildNumber(flowId, buildNumber));
    }

    private static Step newInstance(Job job, Node node) {
        String cmdId = job.getId() + node.getPathAsString();
        Node parent = node.getParent();

        Step step = new Step()
                .setId(StringHelper.toBase64(cmdId))
                .setFlowId(job.getFlowId())
                .setBuildNumber(job.getBuildNumber())
                .setJobId(job.getId())
                .setNodePath(node.getPathAsString())
                .setDockers(node.getDockers())
                .setNext(nextPaths(node))
                .setParent(parent == null ? StringHelper.EMPTY : parent.getPathAsString());

        if (node instanceof ParallelStepNode) {
            step.setType(Step.Type.PARALLEL);
        }

        if (node instanceof RegularStepNode) {
            RegularStepNode r = (RegularStepNode) node;
            step.setAllowFailure(r.isAllowFailure());
            step.setPlugin(r.getPlugin());
            step.setType(Step.Type.STEP);
            step.setPost(r.isPost());

            if (r.hasChildren()) {
                step.setType(Step.Type.STAGE);
            }
        }

        if (node instanceof FlowNode) {
            step.setType(Step.Type.FLOW);
        }

        return step;
    }

    private static List<String> nextPaths(Node node) {
        List<Node> nextList = node.getNext();

        List<String> paths = new ArrayList<>(nextList.size());
        for (Node next : node.getNext()) {
            paths.add(next.getPathAsString());
        }

        return paths;
    }
}
