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
import com.flow.platform.api.domain.Node;
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
@Service(value = "flowService")
public class FlowServiceImpl implements FlowService {

    private final Map<String, Flow> mocFlowList = new HashMap<>();

    @Override
    public Flow create(Flow node) {
        String path = UUID.randomUUID().toString();
        node.setPath(path);
        node.setCreatedAt(new Date());
        node.setUpdatedAt(new Date());
        mocFlowList.put(path, node);
        return node;
    }

    @Override
    public Flow find(String path) {
        return mocFlowList.get(path);
    }

    @Override
    public Boolean destroyFlow(String path) {
        Flow flow = mocFlowList.remove(path);
        if (flow != null) {
            return true;
        } else {
            return false;
        }
    }
}
