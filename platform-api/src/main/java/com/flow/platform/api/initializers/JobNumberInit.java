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

package com.flow.platform.api.initializers;

import com.flow.platform.api.dao.FlowDao;
import com.flow.platform.api.dao.job.JobDao;
import com.flow.platform.api.dao.job.JobNumberDao;
import com.flow.platform.api.domain.job.JobNumber;
import com.flow.platform.api.domain.node.Node;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Init job number since new version is apply
 * job number table to address calculate build number
 *
 * @author yang
 */
@Component
public class JobNumberInit extends Initializer {

    @Autowired
    private JobNumberDao jobNumberDao;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private JobDao jobDao;

    @Override
    void doStart() {
        List<Node> list = flowDao.list();
        for (Node flow : list) {
            initJobNumber(flow);
        }
    }

    private void initJobNumber(Node flow) {
        JobNumber jobNumber = jobNumberDao.get(flow.getPath());

        // Job number has been created
        if (!Objects.isNull(jobNumber)) {
            return;
        }

        // init job number for flow
        Long numOfJob = jobDao.numOfJob(flow.getPath());
        jobNumberDao.save(new JobNumber(flow.getPath(), numOfJob));
    }
}
