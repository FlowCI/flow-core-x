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
import com.flowci.core.flow.domain.SimpleYml;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.plugin.event.GetPluginEvent;
import com.flowci.core.secret.event.GetSecretEvent;
import com.flowci.domain.Vars;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import com.flowci.tree.FlowNode;
import com.flowci.tree.NodeTree;
import com.flowci.tree.YmlParser;
import com.flowci.util.StringHelper;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.ImmutableList;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;


/**
 * @author yang
 */
@Service
public class YmlServiceImpl implements YmlService {

    private final List<NodeElementChecker> elementCheckers = ImmutableList.of(new ConditionChecker(), new ConfigChecker(), new PluginChecker(), new SecretChecker());

    private final Cache<String, NodeTree> flowTreeCache;

    private final YmlDao ymlDao;

    private final FlowDao flowDao;

    private final SpringEventManager eventManager;

    private final ConditionManager conditionManager;

    public YmlServiceImpl(Cache<String, NodeTree> flowTreeCache,
                          YmlDao ymlDao,
                          FlowDao flowDao,
                          SpringEventManager eventManager,
                          ConditionManager conditionManager) {
        this.flowTreeCache = flowTreeCache;
        this.ymlDao = ymlDao;
        this.flowDao = flowDao;
        this.eventManager = eventManager;
        this.conditionManager = conditionManager;
    }

    //====================================================================
    //        %% Public function
    //====================================================================

    @Override
    public List<Yml> list(String flowId) {
        return ymlDao.findAllWithoutRawByFlowId(flowId);
    }

    @Override
    public List<Yml> get(String flowId) {
        var list = ymlDao.findAllByFlowId(flowId);
        if (list.size() == 0) {
            throw new NotFoundException("YAML not found");
        }
        return list;
    }

    @Override
    public NodeTree getTree(String flowId) {
        return flowTreeCache.get(yamlCacheKey(flowId), key -> {
            var list = get(flowId);
            var ymlContentList = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                ymlContentList[i] = StringHelper.fromBase64(list.get(i).getRawInB64());
            }

            FlowNode root = YmlParser.load(ymlContentList);
            return NodeTree.create(root);
        });
    }

    @Override
    public List<Yml> saveYml(Flow flow, SimpleYml... list) {
        var ymlContentList = new String[list.length];
        for (int i = 0; i < list.length; i++) {
            ymlContentList[i] = StringHelper.fromBase64(list[i].getRawInB64());
        }

        FlowNode root = YmlParser.load(ymlContentList);
        NodeTree tree = NodeTree.create(root);

        for (NodeElementChecker checker : elementCheckers) {
            Optional<RuntimeException> exception = checker.apply(tree);
            if (exception.isPresent()) {
                throw exception.get();
            }
        }

        var entities = new ArrayList<Yml>(list.length);
        try {
            for (var s : list) {
                var entity = new Yml(flow.getId(), s.getName(), s.getRawInB64());
                entities.add(ymlDao.save(entity));
            }
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Yaml name or condition already existed");
        }

        // sync flow envs from yml root envs
        Vars<String> vars = flow.getReadOnlyVars();
        vars.clear();
        vars.merge(root.getEnvironments());
        flowDao.save(flow);

        // put tree into cache
        flowTreeCache.put(yamlCacheKey(flow.getId()), tree);
        return entities;
    }

    @Override
    public void delete(String flowId) {
        ymlDao.deleteAllByFlowId(flowId);
    }

    @Override
    public void delete(String flowId, String name) {
        ymlDao.deleteByFlowIdAndName(flowId, name);
    }

    private static String yamlCacheKey(String flowId) {
        return flowId;
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
