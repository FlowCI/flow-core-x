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

/**
 * @author yh@firim
 */
public interface JobService {

    /**
     * run node
     * @param node
     * @return
     */
    Boolean run(Node node);

    /**
     * create agent session
     * @param job
     * @return
     */
    Boolean createSession(Job job);

    /**
     * handle node status
     * @param jobNode
     * @return
     */
    Boolean handleStatus(JobNode jobNode);

    /**
     * handle callback
     * @param job
     * @return
     */
    Boolean handleCreateSessionCallBack(Job job);
    /**
     * create job
     * @param job
     * @return
     */
    Job create(Job job);

    /**
     * find job
     * @param id
     * @return
     */
    Job find(String id);
}
