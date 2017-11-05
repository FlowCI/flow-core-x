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
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.envs.handler.EnvHandler;
import com.flow.platform.core.context.SpringContext;
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

    @PostConstruct
    public void init() {
        // init env handler into map
        String[] beanNameByType = springContext.getBeanNameByType(EnvHandler.class);
        for (String bean : beanNameByType) {
            EnvHandler envHandler = (EnvHandler) springContext.getBean(bean);
            envHandlerMap.put(envHandler.env().name(), envHandler);
        }
    }

    @Override
    public void save(Node node, Map<String, String> envs, boolean verify) {
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
}
