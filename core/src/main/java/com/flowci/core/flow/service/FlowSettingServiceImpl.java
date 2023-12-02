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

import com.flowci.core.common.manager.VarManager;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Settings;
import com.flowci.core.flow.domain.WebhookStatus;
import com.flowci.domain.VarValue;
import com.flowci.common.exception.ArgumentException;
import com.flowci.util.StringHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author yang
 */

@Slf4j
@Service
@AllArgsConstructor
public class FlowSettingServiceImpl implements FlowSettingService {

    private final FlowDao flowDao;

    private final VarManager varManager;

    private final CronService cronService;
    
    @Override
    public void set(Flow flow, Settings settings) {
        if (settings.hasCron()) {
            cronService.validate(settings.getCron());
        }

        flow.setYamlFromRepo(settings.getIsYamlFromRepo());
        flow.setYamlRepoBranch(settings.getYamlRepoBranch());
        flow.setJobTimeout(settings.getJobTimeout());
        flow.setStepTimeout(settings.getStepTimeout());
        flow.setCron(settings.getCron());
        flowDao.save(flow);

        cronService.set(flow);
    }

    @Override
    public void set(Flow flow, WebhookStatus ws) {
        flow.setWebhookStatus(ws);
        flowDao.save(flow);
    }

    @Override
    public void add(Flow flow, Map<String, VarValue> vars) {
        for (Map.Entry<String, VarValue> entry : vars.entrySet()) {
            String name = entry.getKey();
            VarValue value = entry.getValue();

            if (!StringHelper.hasValue(name)) {
                throw new ArgumentException("Var name cannot be empty");
            }

            if (!StringHelper.hasValue(value.getData())) {
                throw new ArgumentException("Var value of {0} cannot be empty", name);
            }

            boolean isVerified = varManager.verify(value.getType(), value.getData());

            if (isVerified) {
                flow.getVars().put(name, value);
                continue;
            }

            throw new ArgumentException("Var {0} format is wrong", name);
        }

        flowDao.save(flow);
    }

    @Override
    public void remove(Flow flow, List<String> vars) {
        for (String key : vars) {
            flow.getVars().remove(key);
        }

        flowDao.save(flow);
    }
}
