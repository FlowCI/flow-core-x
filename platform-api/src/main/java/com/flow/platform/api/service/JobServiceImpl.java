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

import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.JobNode;
import com.flow.platform.api.domain.Node;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yh@firim
 */
@Service(value = "jobService")
public class JobServiceImpl implements JobService{

    private final Map<String, Job> mocJobList = new HashMap<>();

    @Autowired
    private JobNodeService jobNodeService;

    @Autowired
    private NodeService nodeService;

    @Override
    public Boolean handleStatus(JobNode jobNode) {
        return null;
    }

    @Override
    public Job create(Job job) {
        String id = UUID.randomUUID().toString();
        job.setId(id);
        job.setCreatedAt(new Date());
        job.setUpdatedAt(new Date());
        mocJobList.put(id, job);
        return job;
    }

    @Override
    public Job find(String id) {
        return mocJobList.get(id);
    }

    @Override
    public Boolean run(Node node) {
        return null;
    }

    @Override
    public Boolean createSession(Job job) {
        return null;
    }
}
