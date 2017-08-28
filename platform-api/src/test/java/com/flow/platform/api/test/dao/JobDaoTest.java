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

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.job.NodeTag;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.CommonUtil;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class JobDaoTest extends TestBase {

    private Job job;

    private NodeResult nodeResult;

    @Before
    public void before() {
        job = new Job(CommonUtil.randomId());
        job.setCmdId("1111");
        job.setNodePath("flow/test");
        job.setNodeName("test");
        job.setNumber(1);
        job.setSessionId(UUID.randomUUID().toString());
        jobDao.save(job);

        nodeResult = new NodeResult(job.getId(), job.getNodePath());
        nodeResult.setNodeTag(NodeTag.FLOW);
        nodeResult.setStatus(NodeStatus.SUCCESS);
        nodeResultDao.save(nodeResult);
    }

    @Test
    public void should_get_job() {
        // when:
        Job loaded = jobDao.get(job.getId());
        Assert.assertNotNull(loaded);
        Assert.assertNotNull(loaded.getResult());

        // then: check job is correct
        Assert.assertEquals(job.getNodePath(), loaded.getNodePath());
        Assert.assertEquals(nodeResult.getNodeResultKey(), loaded.getResult().getNodeResultKey());

        // then: check job list data
        Assert.assertEquals(job, jobDao.list().get(0));
        Assert.assertEquals(job, jobDao.listByPath(null).get(0));
        Assert.assertEquals(job, jobDao.latestByPath(null).get(0));
    }

    @Test
    public void should_update_job() {
        Job loaded = jobDao.get(job.getId());
        loaded.setNodePath("flow/sss");
        loaded.setNodeName("sss");
        jobDao.update(job);

        Job updated = jobDao.get(job.getId());
        Assert.assertEquals(loaded.getId(), updated.getId());
        Assert.assertNotEquals(loaded.getNodePath(), updated.getNodePath());
    }

    @Test
    public void should_delete_job() {
        Assert.assertEquals(1, jobDao.list().size());

        jobDao.delete(job);
        Assert.assertEquals(0, jobDao.list().size());
    }

    @Test
    public void should_get_max_build_number() {
        Integer number = jobDao.maxBuildNumber(job.getNodePath());
        Assert.assertNotNull(number);
        Assert.assertEquals(1, number.intValue());

        Assert.assertEquals(0, jobDao.maxBuildNumber("flows").intValue());
    }

    @Test
    public void should_list_latest_job_by_path() {
        // given: job
        Job newJob = new Job(CommonUtil.randomId());
        newJob.setCmdId("1111");
        newJob.setNodePath(job.getNodePath());
        newJob.setNodeName(job.getNodeName());
        newJob.setNumber(jobDao.maxBuildNumber(job.getNodePath()) + 1);
        newJob.setSessionId(UUID.randomUUID().toString());
        jobDao.save(newJob);

        // given: node result for new job
        NodeResult newResult = new NodeResult(newJob.getId(), newJob.getNodePath());
        newResult.setNodeTag(NodeTag.FLOW);
        newResult.setStatus(NodeStatus.FAILURE);
        nodeResultDao.save(newResult);

        // when: get latest job
        List<Job> latestJobList  = jobDao.latestByPath(Lists.newArrayList(job.getNodePath()));
        Assert.assertNotNull(latestJobList);
        Assert.assertEquals(1, latestJobList.size());

        // then:
        Job lastJob = latestJobList.get(0);
        Assert.assertEquals(NodeStatus.FAILURE, lastJob.getResult().getStatus());
    }

    @Test
    public void should_list_session_status_success() {
        Job loaded = jobDao.get(job.getId());
        List<String> sessionIds = new ArrayList<>();
        sessionIds.add(loaded.getSessionId());

        List<Job> jobs = jobDao.list(sessionIds, NodeStatus.SUCCESS);
        Assert.assertEquals(1, jobs.size());

        Job job1 = jobs.get(0);
        Assert.assertEquals(nodeResult.getStatus(), job1.getResult().getStatus());
        Assert.assertNotNull(job1.getCreatedAt());
        Assert.assertNotNull(job1.getUpdatedAt());
    }

    @Test
    public void should_list_session_status_success_complex() {
        List<String> sessionIds = new ArrayList<>();
        sessionIds.add(job.getSessionId());

        List<Job> jobs = jobDao.list(sessionIds, NodeStatus.SUCCESS);
        Assert.assertEquals(1, jobs.size());
    }
}
