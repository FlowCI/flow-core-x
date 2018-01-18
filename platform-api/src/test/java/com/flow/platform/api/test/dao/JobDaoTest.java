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
import com.flow.platform.api.domain.job.JobNumber;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.job.NodeTag;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.CommonUtil;
import com.flow.platform.core.domain.Page;
import com.flow.platform.core.domain.Pageable;
import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.EnumSet;
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
        String nodePath = "flow/test";
        jobNumberDao.save(new JobNumber(nodePath));

        job = new Job(CommonUtil.randomId());
        job.setNodePath(nodePath);
        job.setNodeName("test");
        job.setNumber(jobNumberDao.increase(nodePath).getNumber());
        job.setSessionId(UUID.randomUUID().toString());
        jobDao.save(job);

        nodeResult = new NodeResult(job.getId(), job.getNodePath());
        nodeResult.setNodeTag(NodeTag.FLOW);
        nodeResult.setStatus(NodeStatus.SUCCESS);
        nodeResult.setOrder(1);
        nodeResultDao.save(nodeResult);
    }

    @Test
    public void should_get_job() {
        // when:
        Job loaded = jobDao.get(job.getId());
        Assert.assertNotNull(loaded);
        Assert.assertNotNull(loaded.getRootResult());

        // then: check job is correct
        Assert.assertEquals(job.getNodePath(), loaded.getNodePath());
        Assert.assertEquals(nodeResult.getKey(), loaded.getRootResult().getKey());

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
        loaded.setFailureMessage("Failure message");
        jobDao.update(loaded);

        Job updated = jobDao.get(job.getId());
        Assert.assertEquals(loaded.getId(), updated.getId());
        Assert.assertEquals(loaded.getNodePath(), updated.getNodePath());
        Assert.assertEquals("Failure message", updated.getFailureMessage());
    }

    @Test
    public void should_delete_job() {
        Assert.assertEquals(1, jobDao.list().size());

        jobDao.delete(job);
        Assert.assertEquals(0, jobDao.list().size());
    }

    @Test
    public void should_list_job_by_status() {
        // given: job with created status
        Assert.assertEquals(1, jobDao.listByStatus(EnumSet.of(JobStatus.CREATED)).size());

        // when: update job status to SESSION_CREATING
        job.setStatus(JobStatus.SESSION_CREATING);
        jobDao.update(job);

        // then:
        Assert.assertEquals(0, jobDao.listByStatus(EnumSet.of(JobStatus.CREATED)).size());
        Assert.assertEquals(1, jobDao.listByStatus(EnumSet.of(JobStatus.SESSION_CREATING)).size());
    }

    @Test
    public void should_list_latest_job_by_path() {
        // given: job
        Job newJob = new Job(CommonUtil.randomId());
        newJob.setNodePath(job.getNodePath());
        newJob.setNodeName(job.getNodeName());
        newJob.setNumber(jobNumberDao.increase(job.getNodePath()).getNumber());
        newJob.setSessionId(UUID.randomUUID().toString());
        jobDao.save(newJob);

        // given: node result for new job
        NodeResult newResult = new NodeResult(newJob.getId(), newJob.getNodePath());
        newResult.setNodeTag(NodeTag.FLOW);
        newResult.setStatus(NodeStatus.FAILURE);
        newResult.setOrder(1);
        nodeResultDao.save(newResult);

        // when: get latest job
        List<Job> latestJobList  = jobDao.latestByPath(Lists.newArrayList(job.getNodePath()));
        Assert.assertNotNull(latestJobList);
        Assert.assertEquals(1, latestJobList.size());

        // then:
        Job lastJob = latestJobList.get(0);
        Assert.assertEquals(NodeStatus.FAILURE, lastJob.getRootResult().getStatus());
    }

    @Test
    public void should_list_session_status_success() {
        Job loaded = jobDao.get(job.getId());
        List<String> sessionIds = new ArrayList<>();
        sessionIds.add(loaded.getSessionId());

        List<Job> jobs = jobDao.list(sessionIds, NodeStatus.SUCCESS);
        Assert.assertEquals(1, jobs.size());

        Job job1 = jobs.get(0);
        Assert.assertEquals(nodeResult.getStatus(), job1.getRootResult().getStatus());
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

    @Test
    public void should_get_job_by_path_and_number() {
        Job oldJob = jobDao.get(this.job.getNodePath(), this.job.getNumber());

        Assert.assertEquals(oldJob.getId(), job.getId());
        Assert.assertEquals(oldJob.getNodePath(), job.getNodePath());
        Assert.assertEquals(oldJob.getNumber(), job.getNumber());
    }

    @Test
    public void should_page_list() {
        Pageable pageable = new Pageable(1, 10);

        Page<Job> page = jobDao.list(Lists.newArrayList(job.getSessionId()), NodeStatus.SUCCESS, pageable);

        Assert.assertEquals(page.getTotalSize(), 1);
        Assert.assertEquals(page.getPageCount(), 1);
        Assert.assertEquals(page.getPageSize(), 10);
        Assert.assertEquals(page.getPageNumber(), 1);
        Assert.assertEquals(page.getContent().get(0).getId(), job.getId());
    }

    @Test
    public void should_page_list_by_path() {
        Integer count = 5;

        Pageable pageable = new Pageable(1, 2);
        for (int i = 0; i < count; i++) {
            createJob();
        }

        Page<Job> page = jobDao.listByPath(Lists.newArrayList("flow/test"), pageable);

        Assert.assertEquals(page.getTotalSize(), count + 1);
        Assert.assertEquals(page.getPageCount(), 3);
        Assert.assertEquals(page.getPageSize(), 2);
        Assert.assertEquals(page.getPageNumber(), 1);
        Assert.assertEquals(page.getContent().get(0).getNodePath(), job.getNodePath());
    }

    @Test
    public void should_page_list_latest_by_path() {
        Integer count = 5;

        Pageable pageable = new Pageable(1, 10);
        for (int i = 0; i < count; i++) {
            createJob();
        }
        Page<Job> page = jobDao.latestByPath(Lists.newArrayList("flow/test"), pageable);

        Assert.assertEquals(page.getTotalSize(), 1);
        Assert.assertEquals(page.getPageCount(), 1);
        Assert.assertEquals(page.getPageSize(), 10);
        Assert.assertEquals(page.getPageNumber(), 1);
        Assert.assertEquals(page.getContent().get(0).getNodePath(), "flow/test");
    }

    public void createJob() {
        Job newJob = new Job(CommonUtil.randomId());
        newJob.setNodePath(job.getNodePath());
        newJob.setNodeName(job.getNodeName());
        newJob.setNumber(jobNumberDao.increase(job.getNodePath()).getNumber());
        newJob.setSessionId(UUID.randomUUID().toString());
        jobDao.save(newJob);

        NodeResult newResult = new NodeResult(newJob.getId(), newJob.getNodePath());
        newResult.setNodeTag(NodeTag.FLOW);
        newResult.setStatus(NodeStatus.FAILURE);
        newResult.setOrder(1);
        nodeResultDao.save(newResult);
    }

}
