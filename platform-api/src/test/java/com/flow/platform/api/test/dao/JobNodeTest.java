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
package com.flow.platform.api.test.dao;

import com.flow.platform.api.dao.JobNodeDao;
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.JobNode;
import com.flow.platform.api.domain.NodeStatus;
import com.flow.platform.api.domain.NodeTag;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.CommonUtil;
import java.time.ZonedDateTime;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class JobNodeTest extends TestBase {

    @Autowired
    private JobNodeDao jobNodeDao;

    @Test
    public void should_save_and_get_success() {
        Job job = new Job(CommonUtil.randomId());
        JobNode jobNode = new JobNode();
        jobNode.setStatus(NodeStatus.SUCCESS);
        jobNode.setExitCode(0);
        jobNode.setCmdId("1111");
        jobNode.setNodeTag(NodeTag.FLOW);
        jobNode.setPath("/flow");
        jobNode.setJobId(job.getId());
        jobNode.setStartTime(ZonedDateTime.now());
        jobNode.setFinishTime(ZonedDateTime.now());
        jobNodeDao.save(jobNode);

        JobNode job_node = jobNodeDao.get(jobNode.getPath());
        Assert.assertNotNull(job_node);
        Assert.assertEquals("/flow", job_node.getPath());
        Assert.assertEquals(NodeTag.FLOW, job_node.getNodeTag());
    }

    @Test
    public void should_update_success() {
        Job job = new Job(CommonUtil.randomId());
        JobNode jobNode = new JobNode();
        jobNode.setStatus(NodeStatus.SUCCESS);
        jobNode.setExitCode(0);
        jobNode.setCmdId("1111");
        jobNode.setNodeTag(NodeTag.FLOW);
        jobNode.setPath("/flow");
        jobNode.setJobId(job.getId());
        jobNode.setStartTime(ZonedDateTime.now());
        jobNode.setFinishTime(ZonedDateTime.now());
        jobNodeDao.save(jobNode);

        JobNode job_node = jobNodeDao.get(jobNode.getPath());
        job_node.setNodeTag(NodeTag.STEP);
        jobNodeDao.update(job_node);

        JobNode job_node1 = jobNodeDao.get(jobNode.getPath());
        job_node1.setCmdId("22222");
        jobNodeDao.update(job_node1);

        Assert.assertEquals(job_node.getPath(), job_node1.getPath());

    }

    @Test
    public void should_delete_job_node() {
        Job job = new Job(CommonUtil.randomId());
        JobNode jobNode = new JobNode();
        jobNode.setStatus(NodeStatus.SUCCESS);
        jobNode.setExitCode(0);
        jobNode.setCmdId("1111");
        jobNode.setNodeTag(NodeTag.FLOW);
        jobNode.setPath("/flow");
        jobNode.setJobId(job.getId());
        jobNode.setStartTime(ZonedDateTime.now());
        jobNode.setFinishTime(ZonedDateTime.now());
        jobNodeDao.save(jobNode);

        jobNodeDao.delete(jobNode);
        JobNode job_node = jobNodeDao.get(jobNode.getPath());

        Assert.assertEquals(null, job_node);
    }


}
