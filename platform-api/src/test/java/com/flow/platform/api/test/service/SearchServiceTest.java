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

import com.flow.platform.api.domain.SearchCondition;
import com.flow.platform.api.envs.GitEnvs;
import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.util.git.model.GitEventType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class SearchServiceTest extends TestBase {

    @Before
    public void before_test() throws IOException {
        stubDemo();

        Node rootForFlow = createRootFlow("flow1", "flow.yaml");
        Map<String, String> envs = new HashMap<>();
        envs.put(GitEnvs.FLOW_GIT_BRANCH.toString(), "master");
        Job jobManual = jobService.createFromFlowYml(rootForFlow.getPath(), GitEventType.MANUAL, envs, mockUser);

        jobManual.setCreatedBy("yh@fir.im");
        jobDao.update(jobManual);

        Job jobBranchCondition = jobService.createFromFlowYml(rootForFlow.getPath(), GitEventType.MANUAL, envs, mockUser);

        jobBranchCondition.putEnv(GitEnvs.FLOW_GIT_BRANCH, "develop");
        jobBranchCondition.setCreatedBy("will@fir.im");
        jobDao.update(jobBranchCondition);

        Job jobTagCondition = jobService.createFromFlowYml(rootForFlow.getPath(), GitEventType.PR, envs, mockUser);
        jobTagCondition.setCreatedBy("yh@fir.im");
        jobDao.update(jobTagCondition);
    }

    @Test
    public void should_get_all_jobs_success() {
        SearchCondition searchCondition = new SearchCondition(null, null, null);
        List<String> paths = new ArrayList<>();
        paths.add("flow1");
        List<Job> jobs = searchService.search(searchCondition, paths);
        Assert.assertEquals(3, jobs.size());
    }

    @Test
    public void should_get_branch_jobs_success() {
        SearchCondition searchCondition = new SearchCondition(null, "master", null);
        List<String> paths = new ArrayList<>();
        paths.add("flow1");
        List<Job> jobs = searchService.search(searchCondition, paths);
        Assert.assertEquals(3, jobs.size());
    }

    @Test
    public void should_get_event_jobs_success() {
        SearchCondition searchCondition = new SearchCondition(null, null, "PR");
        List<String> paths = new ArrayList<>();
        paths.add("flow1");
        List<Job> jobs = searchService.search(searchCondition, paths);
        Assert.assertEquals(1, jobs.size());
    }

    @Test
    public void should_get_keyword_jobs_success() {
        SearchCondition searchCondition = new SearchCondition("2", null, null);
        List<String> paths = new ArrayList<>();
        paths.add("flow1");
        List<Job> jobs = searchService.search(searchCondition, paths);
        Assert.assertEquals(1, jobs.size());
    }

    @Test
    public void should_get_creator_jobs_success() {
        SearchCondition searchCondition = new SearchCondition(null, null, null, "will@fir.im");
        List<String> paths = new ArrayList<>();
        paths.add("flow1");
        List<Job> jobs = searchService.search(searchCondition, paths);
        Assert.assertEquals(1, jobs.size());
    }

    @Test
    public void should_get_branch_and_pr_jobs_success() {
        SearchCondition searchCondition = new SearchCondition(null, "master", "PR");
        List<String> paths = new ArrayList<>();
        paths.add("flow1");
        List<Job> jobs = searchService.search(searchCondition, paths);
        Assert.assertEquals(1, jobs.size());
    }

}

