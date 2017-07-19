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

import com.flow.platform.api.domain.JobNode;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */
@Service(value = "jobNodeService")
public class JobNodeServiceImpl implements JobNodeService {

    private final Map<String, JobNode> mocJobNodeList = new HashMap<>();
    @Override
    public JobNode create(JobNode jobNode) {
        jobNode.setUpdatedAt(ZonedDateTime.now());
        jobNode.setCreatedAt(ZonedDateTime.now());
        mocJobNodeList.put(jobNode.getPath(), jobNode);
        return jobNode;
    }

    @Override
    public Boolean delete(String path) {
        if(mocJobNodeList.remove(path) == null){
            return false;
        }
        return true;
    }

    @Override
    public JobNode update(JobNode jobNode) {
        jobNode.setUpdatedAt(ZonedDateTime.now());
        mocJobNodeList.put(jobNode.getPath(), jobNode);
        return jobNode;
    }

    @Override
    public JobNode find(String path) {
        return mocJobNodeList.get(path);
    }
}
