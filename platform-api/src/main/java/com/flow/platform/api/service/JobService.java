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
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdBase;

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
     * @param nodePath
     * @return
     */
    Boolean handleCmdResult(Cmd cmd, String nodePath);

    /**
     * create session success
     * @param cmdBase
     * @param jobId
     * @return
     */
    Boolean handleCreateSessionCallBack(CmdBase cmdBase, String jobId);

    /**
     * create job by flow name
     * @param flowName
     * @return
     */
    Job create(String flowName);

    /**
     * find job
     * @param id
     * @return
     */
    Job find(String id);
}
