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

package com.flow.platform.api.service.node;

import com.flow.platform.api.dao.FlowDao;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.envs.AgentEnvs;
import com.flow.platform.api.envs.EnvKey;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.envs.FlowEnvs;
import com.flow.platform.api.envs.GitEnvs;
import com.flow.platform.api.envs.JobEnvs;
import com.flow.platform.api.envs.handler.EnvHandler;
import com.flow.platform.core.context.SpringContext;
import com.flow.platform.core.exception.IllegalOperationException;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service
public class EnvServiceImpl implements EnvService {

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private SpringContext springContext;

    private final Map<String, EnvHandler> envHandlerMap = new HashMap<>(5);

    private final Map<String, EnvKey> envKeyMap = new HashMap<>();

    private final Map<String, EnvKey> editableKeyMap = new HashMap<>();

    private final Map<String, EnvKey> noneEditableKeyMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // init env handler into map
        String[] beanNameByType = springContext.getBeanNameByType(EnvHandler.class);
        for (String bean : beanNameByType) {
            EnvHandler envHandler = (EnvHandler) springContext.getBean(bean);
            envHandlerMap.put(envHandler.env().name(), envHandler);
        }

        // init env list
        Set<EnvKey> keys = Sets.newHashSet(FlowEnvs.values());
        keys.addAll(Sets.newHashSet(GitEnvs.values()));
        keys.addAll(Sets.newHashSet(JobEnvs.values()));
        keys.addAll(Sets.newHashSet(AgentEnvs.values()));
        for (EnvKey key : keys) {
            envKeyMap.put(key.name(), key);

            if (key.isEditable()) {
                editableKeyMap.put(key.name(), key);
            } else {
                noneEditableKeyMap.put(key.name(), key);
            }
        }
    }

    @Override
    public Map<String, String> list(Node node, boolean editable) {
        HashMap<String, String> envs = Maps.newHashMap(node.getEnvs());

        if (editable) {
            for (String key : noneEditableKeyMap.keySet()) {
                envs.remove(key);
            }
        }

        else {
            for (String key : editableKeyMap.keySet()) {
                envs.remove(key);
            }
        }

        return envs;
    }

    @Override
    public void save(Node node, Map<String, String> envs, boolean verify) {
        if (verify) {
            verifyWhenAdd(envs);
        }

        EnvUtil.merge(envs, node.getEnvs(), true);

        // handle envs before save
        for (Map.Entry<String, String> entry : node.getEnvs().entrySet()) {
            EnvHandler envHandler = envHandlerMap.get(entry.getKey());
            if (envHandler != null) {
                envHandler.handle(node);
            }
        }

        // sync latest env into flow table
        flowDao.update(node);
    }

    @Override
    public void delete(Node node, Set<String> keys, boolean verify) {
        if (verify) {
            verifyWhenDelete(keys);
        }

        // handle envs before delete
        for (String env : keys) {
            EnvHandler envHandler = envHandlerMap.get(env);
            if (envHandler != null && node.getEnvs().containsKey(env)) {
                envHandler.unHandle(node);
            }
        }

        // remove env
        for (String keyToRemove : keys) {
            node.removeEnv(keyToRemove);
        }

        // sync latest env into flow table
        flowDao.update(node);
    }

    private void verifyWhenAdd(Map<String, String> envs) {
        for (Map.Entry<String, String> entry : envs.entrySet()) {
            EnvKey key = envKeyMap.get(entry.getKey());
            if (key == null) {
                continue;
            }

            if (key.isReadonly()) {
                throw new IllegalOperationException(String.format("The env '%s' is readonly", key.name()));
            }

            Set<String> values = key.availableValues();
            if (values == null) {
                continue;
            }

            if (!values.contains(entry.getValue())) {
                throw new IllegalOperationException(
                    String.format("The value '%s' is not acceptable for env '%s", entry.getValue(), key.name()));
            }
        }
    }

    private void verifyWhenDelete(Set<String> keys) {
        for (String key : keys) {
            EnvKey envKey = envKeyMap.get(key);
            if (envKey == null) {
                continue;
            }

            if (envKey.isReadonly()) {
                throw new IllegalOperationException(String.format("The env '%s' is readonly", envKey.name()));
            }
        }
    }
}
