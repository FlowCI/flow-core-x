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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.envs.GitEnvs;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.job.NodeResult;
import com.flow.platform.api.domain.job.NodeStatus;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.git.model.GitEventType;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * @author yh@firim
 */
public class JobControllerTest extends TestBase {

    @Autowired
    private Path workspace;

    @Test
    public void should_show_job_success() throws Exception {
        stubDemo();
        Node rootForFlow = createRootFlow("flow1", "flow.yaml");
        Job job = jobService.createFromFlowYml(rootForFlow.getPath(), JobCategory.MANUAL, null, mockUser);
        job.putEnv(GitEnvs.FLOW_GIT_BRANCH, "master");
        jobDao.update(job);

        Job returnedJob = requestToShowJob(job.getNodePath(), job.getNumber());

        // then: verify children result is returned
        Assert.assertNotNull(returnedJob.getChildrenResult());

        // then: verify session id is created
        Assert.assertNull(returnedJob.getSessionId());

        // when: load yml
        String ymlForJob = requestToGetYml(job.getNodePath(), job.getNumber());
        String originYml = getResourceContent("flow.yaml");
        Assert.assertEquals(originYml, ymlForJob);
    }

    @Test
    public void should_stop_job_success() throws Exception {
        stubDemo();
        Node rootForFlow = createRootFlow("flow1", "flow.yaml");
        Job job = jobService.createFromFlowYml(rootForFlow.getPath(), JobCategory.TAG, null, mockUser);
        job.putEnv(GitEnvs.FLOW_GIT_BRANCH, "master");
        jobDao.update(job);

        MvcResult mvcResult = this.mockMvc.perform(
            post(String.format("/jobs/%s/%s/stop", job.getNodeName(), job.getNumber()))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        Job jobLoaded = Jsonable.GSON_CONFIG.fromJson(response, Job.class);
        Assert.assertNotNull(jobLoaded);

        Job loadedJob = requestToShowJob(job.getNodePath(), job.getNumber());
        Assert.assertEquals(NodeStatus.STOPPED, loadedJob.getRootResult().getStatus());
    }

    @Test
    public void should_get_step_log_success() throws Exception {
        stubDemo();
        Node rootForFlow = createRootFlow("flow1", "flow.yaml");
        Job job = jobService.createFromFlowYml(rootForFlow.getPath(), JobCategory.TAG, null, mockUser);
        job.putEnv(GitEnvs.FLOW_GIT_BRANCH, "master");
        jobDao.update(job);

        MvcResult mvcResult = this.mockMvc.perform(
            get(String.format("/jobs/%s/%s/1/log", job.getNodeName(), job.getNumber()))
        ).andExpect(status().isOk()).andReturn();
        String response = mvcResult.getResponse().getContentAsString();
        Assert.assertNotNull(response);
    }

    @Test
    public void should_get_job_zip_error() throws Exception {
        stubDemo();
        Node rootForFlow = createRootFlow("flow1", "flow.yaml");
        Job job = jobService.createFromFlowYml(rootForFlow.getPath(), JobCategory.TAG, null, mockUser);

        job.putEnv(GitEnvs.FLOW_GIT_BRANCH, "master");
        jobDao.update(job);
        MvcResult mvcResult = this.mockMvc.perform(
            get(String.format("/jobs/%s/%s/log/download", job.getNodeName(), job.getNumber()))
        ).andExpect(status().isBadRequest()).andReturn();
        String response = mvcResult.getResponse().getContentAsString();
        Assert.assertNotNull(response);
    }

    @Test
    public void should_get_job_zip_success() throws Exception {
        stubDemo();
        Node rootForFlow = createRootFlow("flow1", "flow.yaml");
        Job job = jobService.createFromFlowYml(rootForFlow.getPath(), JobCategory.TAG, null, mockUser);

        job.putEnv(GitEnvs.FLOW_GIT_BRANCH, "master");
        job.setStatus(JobStatus.SUCCESS);
        jobDao.update(job);

        List<NodeResult> nodeResultList = nodeResultService.list(job, true);
        for (NodeResult nodeResult : nodeResultList) {
            nodeResult.setCmdId("xxxx");
            nodeResultService.update(nodeResult);
        }

        MvcResult mvcResult = this.mockMvc.perform(
            get(String.format("/jobs/%s/%s/log/download", job.getNodeName(), job.getNumber()))
        ).andExpect(status().isOk()).andReturn();
        String response = mvcResult.getResponse().getContentAsString();
        Assert.assertNotNull(response);

        Path zipLog = Paths
            .get(workspace.toString(), job.getNodeName(), "log", job.getId().toString(),
                job.getId().toString() + ".zip");
        File zipFile = new File(zipLog.toString());
        Assert.assertEquals(true, zipFile.exists());
    }

    private Job requestToShowJob(String path, Integer buildNumber) throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(
            get(String.format("/jobs/%s/%s", path, buildNumber))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        return Job.parse(response, Job.class);
    }

    private String requestToGetYml(String path, Integer buildNumber) throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get(String.format("/jobs/%s/%s/yml", path, buildNumber)))
            .andExpect(status().isOk())
            .andReturn();

        return mvcResult.getResponse().getContentAsString();
    }

}
