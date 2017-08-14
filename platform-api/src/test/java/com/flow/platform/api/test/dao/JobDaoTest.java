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

import com.flow.platform.api.dao.JobDao;
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.NodeStatus;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.CommonUtil;
import java.time.ZonedDateTime;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class JobDaoTest extends TestBase {

    @Autowired
    private JobDao jobDao;

    @Test
    public void should_save_and_get_success() {
        Job job = new Job(CommonUtil.randomId());
        job.setStatus(NodeStatus.SUCCESS);
        job.setExitCode(0);
        job.setCmdId("1111");
        job.setNodePath("/flow/test");
        job.setStartedAt(ZonedDateTime.now());
        job.setFinishedAt(ZonedDateTime.now());
        jobDao.save(job);

        Job job1 = jobDao.get(job.getId());
        Assert.assertNotNull(job1);
        Assert.assertEquals(job.getNodePath(), job1.getNodePath());
    }

    @Test
    public void should_update_success() {
        Job job = new Job(CommonUtil.randomId());
        job.setStatus(NodeStatus.SUCCESS);
        job.setExitCode(0);
        job.setCmdId("1111");
        job.setNodePath("/flow/test");
        job.setStartedAt(ZonedDateTime.now());
        job.setFinishedAt(ZonedDateTime.now());
        jobDao.save(job);

        Job job1 = jobDao.get(job.getId());
        job.setNodePath("/flow/sss");
        jobDao.update(job);

        Job job2 = jobDao.get(job.getId());
        Assert.assertEquals(job1.getId(), job2.getId());
        Assert.assertNotEquals(job1.getNodePath(), job2.getNodePath());
    }

    @Test
    public void should_list_success() {
        Job job = new Job(CommonUtil.randomId());
        job.setStatus(NodeStatus.SUCCESS);
        job.setExitCode(0);
        job.setCmdId("1111");
        job.setNodePath("/flow/test");
        job.setStartedAt(ZonedDateTime.now());
        job.setFinishedAt(ZonedDateTime.now());
        jobDao.save(job);
        Assert.assertEquals(1, jobDao.list().size());
    }

    @Test
    public void should_delete_success() {
        Job job = new Job(CommonUtil.randomId());
        job.setStatus(NodeStatus.SUCCESS);
        job.setExitCode(0);
        job.setCmdId("1111");
        job.setNodePath("/flow/test");
        job.setStartedAt(ZonedDateTime.now());
        job.setFinishedAt(ZonedDateTime.now());
        jobDao.save(job);
        Assert.assertEquals(1, jobDao.list().size());
        jobDao.delete(job);
        Assert.assertEquals(0, jobDao.list().size());
    }

    @Test
    public void should_get_max_number_success(){
        Job job = new Job(CommonUtil.randomId());
        job.setNumber(1);
        job.setStatus(NodeStatus.SUCCESS);
        job.setExitCode(0);
        job.setCmdId("1111");
        job.setNodePath("/flow/test");
        job.setNodeName("test");
        job.setStartedAt(ZonedDateTime.now());
        job.setFinishedAt(ZonedDateTime.now());

        jobDao.save(job);

        Integer number = jobDao.maxBuildNumber(job.getNodeName());
        Assert.assertNotNull(number);

        Assert.assertEquals((Integer)0, jobDao.maxBuildNumber("flows"));
    }
}
