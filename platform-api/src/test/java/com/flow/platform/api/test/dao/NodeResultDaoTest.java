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

import static junit.framework.TestCase.fail;

import com.flow.platform.api.dao.job.NodeResultDao;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeResultKey;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.job.NodeTag;
import com.flow.platform.api.service.job.NodeResultService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.util.CommonUtil;
import com.flow.platform.core.domain.Page;
import com.flow.platform.core.domain.Pageable;
import com.flow.platform.core.exception.NotFoundException;
import java.time.ZonedDateTime;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lhl
 */
public class NodeResultDaoTest extends TestBase {

    @Autowired
    private NodeResultService nodeResultService;

    @Autowired
    private NodeResultDao nodeResultDao;

    @Test
    public void should_save_and_get_success() {
        Job job = new Job(CommonUtil.randomId());
        job.setNodePath("/flow1");

        NodeResult jobNode = new NodeResult(job.getId(), "/flow1");
        jobNode.setStatus(NodeStatus.SUCCESS);
        jobNode.setExitCode(0);
        jobNode.setCmdId("1111");
        jobNode.setNodeTag(NodeTag.FLOW);
        jobNode.setOrder(1);
        jobNode.setFailureMessage("Error message");
        jobNode.setStartTime(ZonedDateTime.now());
        jobNode.setFinishTime(ZonedDateTime.now());
        nodeResultDao.save(jobNode);

        NodeResultKey nodeResultKey = new NodeResultKey(job.getId(), job.getNodePath());
        NodeResult job_node = nodeResultDao.get(nodeResultKey);
        Assert.assertNotNull(job_node);
        Assert.assertEquals("/flow1", job_node.getKey().getPath());
        Assert.assertEquals(NodeTag.FLOW, job_node.getNodeTag());
        Assert.assertEquals("Error message", jobNode.getFailureMessage());

        // get node result by job id and order
        NodeResult result = nodeResultDao.get(job.getId(), 1);
        Assert.assertEquals(jobNode, result);
    }

    @Test
    public void should_update_success() {
        Job job = new Job(CommonUtil.randomId());
        job.setNodePath("/flow1");

        NodeResult jobNode = new NodeResult(job.getId(), "/flow1");
        jobNode.setStatus(NodeStatus.SUCCESS);
        jobNode.setExitCode(0);
        jobNode.setCmdId("1111");
        jobNode.setOrder(1);
        jobNode.setNodeTag(NodeTag.FLOW);
        jobNode.setStartTime(ZonedDateTime.now());
        jobNode.setFinishTime(ZonedDateTime.now());
        nodeResultDao.save(jobNode);

        NodeResult job_node = nodeResultService.find(job.getNodePath(), job.getId());
        job_node.setNodeTag(NodeTag.STEP);
        nodeResultDao.update(job_node);

        NodeResult job_node1 = nodeResultService.find(job.getNodePath(), job.getId());
        job_node1.setCmdId("22222");
        nodeResultDao.update(job_node1);

        Assert.assertEquals(job_node.getKey().getPath(), job_node1.getKey().getPath());
    }

    @Test
    public void should_delete_job_node() {
        Job job = new Job(CommonUtil.randomId());
        NodeResult jobNode = new NodeResult(job.getId(), "/flow");
        jobNode.setStatus(NodeStatus.SUCCESS);
        jobNode.setExitCode(0);
        jobNode.setCmdId("1111");
        jobNode.setNodeTag(NodeTag.FLOW);
        jobNode.setOrder(1);
        jobNode.setStartTime(ZonedDateTime.now());
        jobNode.setFinishTime(ZonedDateTime.now());
        nodeResultDao.save(jobNode);

        nodeResultDao.delete(jobNode);
        try {
            nodeResultService.find(job.getNodePath(), job.getId());
            fail();
        } catch (NotFoundException e) {
            Assert.assertEquals(NotFoundException.class, e.getClass());
        }
    }

    @Test
    public void should_page_list_by_job_id(){
        Pageable pageable = new Pageable(1,2);

        Job job = new Job(CommonUtil.randomId());

        Integer count = 5;

        for(int i = 0;i <count; i++){
            NodeResult jobNode = new NodeResult(job.getId(), "/flow"+i);
            jobNode.setStatus(NodeStatus.SUCCESS);
            jobNode.setExitCode(0);
            jobNode.setCmdId("1111");
            jobNode.setNodeTag(NodeTag.FLOW);
            jobNode.setOrder(1);
            jobNode.setStartTime(ZonedDateTime.now());
            jobNode.setFinishTime(ZonedDateTime.now());
            nodeResultDao.save(jobNode);
        }

        Page<NodeResult> page = nodeResultDao.list(job.getId(), pageable);

        Assert.assertEquals(page.getTotalSize(),5);
        Assert.assertEquals(page.getPageCount(),3);
        Assert.assertEquals(page.getPageSize(),2);
        Assert.assertEquals(page.getPageNumber(),1);
        Assert.assertEquals(page.getContent().size(),2);
    }

}
