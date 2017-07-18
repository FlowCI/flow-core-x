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

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Step;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */
@Service(value = "stepService")
public class StepServiceImpl implements StepService {

    private final Map<String, Map<String, Step>> mocStepList = new HashMap<>();

    @Override
    public Step create(Step node, Flow flow) {
        String path = UUID.randomUUID().toString();
        node.setPath(path);
        node.setCreatedAt(new Date());
        node.setUpdatedAt(new Date());
        List<Step> steps = list(flow.getPath());
        steps.add(node);
        mocStepList.put(flow.getPath(), update(flow.getPath(), node.getPath(), node));
        return node;
    }

    @Override
    public List<Step> list(String flowPath) {
        Map<String, Step> steps = mocStepList.get(flowPath);
        if(steps == null){
            steps = new HashMap<>();
        }

        List<Step> stepList = new LinkedList<>();
        for(Step step : steps.values()){
            stepList.add(step);
        }
        return stepList;
    }

    @Override
    public List<Step> list(Flow flow) {
        return list(flow.getPath());
    }

    @Override
    public Boolean delete(Step step, Flow flow) {
        List<Step> steps = list(flow.getPath());
        steps.remove(step);
        mocStepList.put(flow.getPath(), remove(flow.getPath(), step.getPath()));
        return true;
    }

    @Override
    public Boolean delete(String name, Flow flow) {
        return delete(find(name, flow), flow);
    }

    @Override
    public Step find(String stepPath, Flow flow) {
        Map<String, Step> steps = mocStepList.get(flow.getPath());
        if(steps == null){
            steps = new HashMap<>();
        }
        return steps.get(stepPath);
    }

    private Map<String, Step> update(String flowPath, String stepPath, Step step){
        Map<String, Step> steps = mocStepList.get(flowPath);
        if(steps == null){
            steps = new HashMap<>();
        }
        steps.put(stepPath, step);
        return steps;
    }

    private Map<String, Step> remove(String flowPath, String stepPath){
        Map<String, Step> steps = mocStepList.get(flowPath);
        if(steps == null){
            steps = new HashMap<>();
        }
        steps.remove(stepPath);
        return steps;
    }
}
