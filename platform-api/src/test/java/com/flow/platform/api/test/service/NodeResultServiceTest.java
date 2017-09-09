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

import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeTag;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.CommonUtil;
import java.io.IOException;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author lhl
 */
public class NodeResultServiceTest extends TestBase {

    @Test
    public void should_save_job_node_by_job() throws IOException {
        // when: create node result list from job
        stubDemo();
        Node rootForFlow = createRootFlow("flow1", "flow.yaml");
        Job job = jobService.createJob(rootForFlow.getPath());

        // then: check node result is created
        List<NodeResult> list = nodeResultService.list(job);
        Assert.assertEquals(5, list.size());

        // then: check cmd id is defined
        for (NodeResult nodeResult : list) {
            Assert.assertEquals(String.format("%s-%s", job.getId(), nodeResult.getKey().getPath()),
                nodeResult.getCmdId());
        }

        // then: check flow node result is created
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
        nodeResultService.save(nodeResult);
        Assert.assertEquals(nodeResult.getNodeTag(), NodeTag.STEP);
    }
}
