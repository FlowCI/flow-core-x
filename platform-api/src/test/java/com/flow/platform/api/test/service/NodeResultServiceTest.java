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

import com.flow.platform.api.dao.JobDao;
import com.flow.platform.api.dao.NodeResultDao;
import com.flow.platform.api.dao.YmlStorageDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.NodeResult;
import com.flow.platform.api.domain.NodeTag;
import com.flow.platform.api.domain.YmlStorage;
import com.flow.platform.api.service.NodeResultService;
import com.flow.platform.api.service.JobService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.test.util.NodeUtilYmlTest;
import com.flow.platform.api.util.CommonUtil;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class NodeResultServiceTest extends TestBase {

    @Autowired
    private NodeResultService nodeResultService;

    @Autowired
    private NodeResultDao nodeResultDao;

    @Autowired
    private JobService jobService;

    @Autowired
    private YmlStorageDao ymlStorageDao;

    @Autowired
    private JobDao jobDao;

    @Test
    public void should_save_job_node_by_job() throws IOException{
        ClassLoader classLoader = NodeUtilYmlTest.class.getClassLoader();
        URL resource = classLoader.getResource("flow.yaml");
        File path = new File(resource.getFile());
        String ymlString = Files.toString(path, Charset.forName("UTF-8"));
        YmlStorage storage = new YmlStorage("/flow1", ymlString);
        ymlStorageDao.save(storage);
        Flow flow = new Flow("/flow1", "flow");
        Job job = jobService.createJob(flow.getPath());
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
