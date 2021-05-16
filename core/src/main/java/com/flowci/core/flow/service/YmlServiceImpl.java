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
import com.flowci.core.config.event.GetConfigEvent;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.YmlDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.plugin.event.GetPluginEvent;
import com.flowci.core.secret.event.GetSecretEvent;
import com.flowci.domain.Vars;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import com.flowci.tree.FlowNode;
import com.flowci.tree.NodeTree;
import com.flowci.tree.YmlParser;
import com.flowci.util.StringHelper;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;


/**
 * @author yang
 */
@Service
public class YmlServiceImpl implements YmlService {

    private final List<NodeElementChecker> elementCheckers = ImmutableList.of(
            new ConditionChecker(),
            new ConfigChecker(),
            new PluginChecker(),
            new SecretChecker()
    );

    @Autowired
    private Cache<String, NodeTree> flowTreeCache;

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
    public NodeTree getTree(String flowId, String name) {
        Yml yml = getYml(flowId, name);
        FlowNode root = YmlParser.load(yml.getRaw());
        return flowTreeCache.get(yamlCacheKey(flowId, name), key -> NodeTree.create(root));
    }

    @Override
    public Yml getYml(String flowId, String name) {
        Optional<Yml> optional = ymlDao.findByFlowIdAndName(flowId, name);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("YAML not found");
    }

    @Override
    public String getYmlString(String flowId, String name) {
        Yml yml = getYml(flowId, name);
        return yml.getRawInB64();
    }

    @Override
    public Yml saveYml(Flow flow, String name, String ymlInB64) {
        if (!StringHelper.hasValue(ymlInB64)) {
            throw new ArgumentException("YAML content cannot be null or empty");
        }

        String yaml = StringHelper.fromBase64(ymlInB64);
        FlowNode root = YmlParser.load(yaml);
        NodeTree tree = NodeTree.create(root);

        for (NodeElementChecker checker : elementCheckers) {
            Optional<RuntimeException> exception = checker.apply(tree);
            if (exception.isPresent()) {
                throw exception.get();
            }
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

        // put tree into cache
        flowTreeCache.put(yamlCacheKey(flow.getId(), name), tree);
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

    private String yamlCacheKey(String flowId, String name) {
        return flowId + "-" + name;
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

    private interface NodeElementChecker extends Function<NodeTree, Optional<RuntimeException>> {

    }

    private class ConditionChecker implements NodeElementChecker {

        @Override
        public Optional<RuntimeException> apply(NodeTree tree) {
            try {
                for (String c : tree.getConditions()) {
                    conditionManager.verify(c);
                }
                return Optional.empty();
            } catch (Throwable e) {
                return Optional.of(new RuntimeException(e.getMessage()));
            }
        }
    }

    private class PluginChecker implements NodeElementChecker {

        @Override
        public Optional<RuntimeException> apply(NodeTree tree) {
            for (String p : tree.getPlugins()) {
                GetPluginEvent event = eventManager.publish(new GetPluginEvent(this, p));
                if (event.hasError()) {
                    return Optional.of(event.getError());
                }
            }
            return Optional.empty();
        }
    }

    private class SecretChecker implements NodeElementChecker {

        @Override
        public Optional<RuntimeException> apply(NodeTree tree) {
            for (String s : tree.getSecrets()) {
                GetSecretEvent event = eventManager.publish(new GetSecretEvent(this, s));
                if (event.hasError()) {
                    return Optional.of(event.getError());
                }
            }
            return Optional.empty();
        }
    }

    private class ConfigChecker implements NodeElementChecker {

        @Override
        public Optional<RuntimeException> apply(NodeTree tree) {
            for (String c : tree.getConfigs()) {
                GetConfigEvent event = eventManager.publish(new GetConfigEvent(this, c));
                if (event.hasError()) {
                    return Optional.of(event.getError());
                }
            }
            return Optional.empty();
        }
    }
}
