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

package com.flow.platform.api.test.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.flow.platform.api.dao.YmlStorageDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.domain.Job;
import com.flow.platform.api.domain.YmlStorage;
import com.flow.platform.api.service.JobService;
import com.flow.platform.api.service.NodeService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.api.test.util.NodeUtilYmlTest;
import com.flow.platform.api.util.NodeUtil;
import com.google.common.io.Files;
import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * @author yh@firim
 */
public class JobControllerTest extends TestBase {

    @Autowired
    private NodeService nodeService;

    @Autowired
    private JobService jobService;

    @Autowired
    private YmlStorageDao ymlStorageDao;

    @Test
    public void should_show_job_success() throws Exception {
        stubDemo();
        ClassLoader classLoader = NodeUtilYmlTest.class.getClassLoader();
        URL resource = classLoader.getResource("flow.yaml");
        File path = new File(resource.getFile());
        String ymlString = Files.toString(path, Charset.forName("UTF-8"));
        YmlStorage storage = new YmlStorage("/flow1", ymlString);
        ymlStorageDao.save(storage);
        Flow flow = (Flow) NodeUtil.buildFromYml(path);
        nodeService.create(flow);
        Job job = jobService.createJob(flow.getPath());

        MockHttpServletRequestBuilder content = get(new StringBuffer("/jobs/").append(job.getId()).toString())
            .contentType(MediaType.APPLICATION_JSON);
        ResultHandler resultHandler;
        MvcResult mvcResult = this.mockMvc.perform(content)
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
//        String s = response.getContentAsString();
//        Job job1 = Jsonable.parse(s, Job.class);
//        Assert.assertEquals(job1 .getId(), job.getId());
    }

}
