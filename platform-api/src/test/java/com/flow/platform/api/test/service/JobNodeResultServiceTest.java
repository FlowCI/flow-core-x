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
package com.flow.platform.api.test.service;

import com.flow.platform.api.dao.NodeResultDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.NodeResult;
import com.flow.platform.api.domain.NodeTag;
import com.flow.platform.api.service.JobService;
import com.flow.platform.api.service.JobNodeResultService;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.CommonUtil;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class JobNodeResultServiceTest extends TestBase {

    @Autowired
    private JobNodeResultService nodeResultService;

    @Autowired
    private NodeResultDao nodeResultDao;

    @Autowired
    private JobService jobService;

    @Autowired
    private NodeService nodeService;

    @Test
    public void should_save_job_node_by_job() throws IOException {
        stubDemo();
        Job job = jobService.createJob(getResourceContent("flow.yaml"));
        Flow flow = (Flow) nodeService.find(job.getNodePath());
        Assert.assertEquals(job.getId(), nodeResultService.find(flow.getPath(), job.getId()).getJobId());
    }

    @Test
    public void should_update_job_node() {
        Job job = new Job(CommonUtil.randomId());
        NodeResult nodeResult = new NodeResult(job.getId(), "/flow_test");
        nodeResult.setNodeTag(NodeTag.FLOW);
        nodeResultDao.save(nodeResult);

        nodeResult.setNodeTag(NodeTag.STEP);
        NodeResult nodeResult1 = nodeResultService.update(nodeResult);
        Assert.assertEquals(nodeResult1.getNodeTag(), NodeTag.STEP);
    }
}
