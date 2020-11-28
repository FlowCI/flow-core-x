/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.flow.service;

import com.flowci.core.common.manager.ConditionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.YmlDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.plugin.event.GetPluginEvent;
import com.flowci.domain.LocalTask;
import com.flowci.domain.Vars;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import com.flowci.tree.FlowNode;
import com.flowci.tree.Node;
import com.flowci.tree.StepNode;
import com.flowci.tree.YmlParser;
import com.flowci.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author yang
 */
@Service
public class YmlServiceImpl implements YmlService {

    @Autowired
    private YmlDao ymlDao;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private ConditionManager conditionManager;

    //====================================================================
    //        %% Public function
    //====================================================================

    @Override
    public List<Yml> list(String flowId) {
        return ymlDao.findAllWithoutRawByFlowId(flowId);
    }

    @Override
    public List<Yml> listWithRaw(String flowId) {
        return ymlDao.findAllByFlowId(flowId);
    }

    @Override
    public FlowNode getRaw(String flowId, String name) {
        Yml yml = getYml(flowId, name);
        return YmlParser.load(name, yml.getRaw());
    }

    @Override
    public Yml getYml(String flowId, String name) {
        Optional<Yml> optional = ymlDao.findByFlowIdAndName(flowId, name);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("yml not found");
    }

    @Override
    public String getYmlString(String flowId, String name) {
        Yml yml = getYml(flowId, name);
        return yml.getRawInB64();
    }

    @Override
    public Yml saveYml(Flow flow, String name, String ymlInB64) {
        if (!StringHelper.hasValue(ymlInB64)) {
            throw new ArgumentException("Yml content cannot be null or empty");
        }

        String yaml = StringHelper.fromBase64(ymlInB64);
        FlowNode root = YmlParser.load(flow.getName(), yaml);
        Optional<RuntimeException> hasErr = verifyPlugins(root);
        if (hasErr.isPresent()) {
            throw hasErr.get();
        }

        hasErr = verifyCondition(root);
        if (hasErr.isPresent()) {
            throw hasErr.get();
        }

        Yml ymlObj = getOrCreate(flow.getId(), name, ymlInB64);

        try {
            ymlDao.save(ymlObj);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Yaml name or condition already existed");
        }

        // sync flow envs from yml root envs
        Vars<String> vars = flow.getVariables();
        vars.clear();
        vars.merge(root.getEnvironments());
        flowDao.save(flow);

        return ymlObj;
    }

    @Override
    public void delete(String flowId) {
        ymlDao.deleteAllByFlowId(flowId);
    }

    @Override
    public void delete(String flowId, String name) {
        ymlDao.deleteByFlowIdAndName(flowId, name);
    }

    private Yml getOrCreate(String flowId, String name, String ymlInB64) {
        Optional<Yml> optional = ymlDao.findByFlowIdAndName(flowId, name);
        if (optional.isPresent()) {
            Yml yml = optional.get();
            yml.setRawInB64(ymlInB64);
            return yml;
        }
        return new Yml(flowId, name, ymlInB64);
    }

    private Optional<RuntimeException> verifyCondition(Node node) {
        try {
            if (node.hasCondition()) {
                conditionManager.verify(node.getCondition());
            }

            for (Node child : node.getChildren()) {
                Optional<RuntimeException> exception = verifyCondition(child);
                if (exception.isPresent()) {
                    return exception;
                }
            }

            return Optional.empty();
        } catch (Throwable e) {
            return Optional.of(new RuntimeException(e.getMessage()));
        }
    }

    /**
     * Try to fetch plugins
     *
     * @return Optional exception
     */
    private Optional<RuntimeException> verifyPlugins(FlowNode flowNode) {
        Set<String> plugins = new HashSet<>();

        for (StepNode step : flowNode.getChildren()) {
            if (step.hasPlugin()) {
                plugins.add(step.getPlugin());
            }
        }

        for (LocalTask t : flowNode.getNotifications()) {
            plugins.add(t.getPlugin());
        }

        for (String plugin : plugins) {
            GetPluginEvent event = eventManager.publish(new GetPluginEvent(this, plugin));
            if (event.hasError()) {
                return Optional.of(event.getError());
            }
        }

        return Optional.empty();
    }
}
