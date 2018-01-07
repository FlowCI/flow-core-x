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

import com.flow.platform.api.dao.FlowDao;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.domain.Page;
import com.flow.platform.core.domain.Pageable;
import com.flow.platform.core.domain.PageableImpl;
import com.google.common.collect.Sets;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yh@firim
 */
public class FlowDaoTest extends TestBase {

    @Autowired
    private FlowDao flowDao;

    private Node flow;

    @Before
    public void before() {
        flow = new Node("/flow", "flow");
        flow.setCreatedAt(ZonedDateTime.now());
        flow.setUpdatedAt(ZonedDateTime.now());
        flow.setCreatedBy("admin@flow.ci");
        flowDao.save(flow);
    }

    @Test
    public void should_list_success(){
        List<Node> flows = flowDao.list();
        Assert.assertEquals(1, flows.size());
        Assert.assertEquals(flow, flows.get(0));
    }

    @Test
    public void should_list_page_success(){

        Pageable pageable = new PageableImpl(0, 10);
        Page<Node> page = flowDao.list(pageable);
        Assert.assertNotNull(page);
        Assert.assertEquals(1, page.getContent().size());
    }

    @Test
    public void should_save_and_get_success(){
        Node flowCp = flowDao.get(flow.getPath());
        Assert.assertNotNull(flowCp);

        Assert.assertEquals(flow, flowCp);
        Assert.assertEquals(flow.getName(), flowCp.getName());
    }

    @Test
    public void should_update_success(){
        Assert.assertEquals(1, flowDao.list().size());

        flow.setName("flow1");
        flowDao.update(flow);
        Assert.assertEquals(1, flowDao.list().size());

        Node flowCp = flowDao.get(flow.getPath());
        Assert.assertEquals(flow.getName(), flowCp.getName());
    }

    @Test
    public void should_delete_success(){
        Assert.assertEquals(1, flowDao.list().size());
        flowDao.delete(flow);

        Node flowCp = flowDao.get(flow.getPath());
        Assert.assertEquals(null, flowCp);
        Assert.assertEquals(0, flowDao.list().size());
    }

    @Test
    public void should_list_flow_by_created_by() {
        List<String> paths = flowDao.pathList(Sets.newHashSet("admin@flow.ci"));
        Assert.assertEquals(1, paths.size());
        Assert.assertEquals(flow.getPath(), paths.get(0));
    }
}
