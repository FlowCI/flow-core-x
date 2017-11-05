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

package com.flow.platform.api.test.integration;

import com.flow.platform.api.domain.job.Job;
import com.flow.platform.api.domain.job.JobCategory;
import com.flow.platform.api.domain.job.JobStatus;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.envs.GitEnvs;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.util.ObjectWrapper;
import com.flow.platform.util.git.model.GitSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class ManualJobTest extends TestBase {

    @Test
    public void should_manual_create_job_with_failure_since_unable_to_create_session() throws Throwable {
        // given: flow
        Node flow = nodeService.createEmptyFlow("manual_flow_test");
        setFlowToReady(flow);

        Map<String, String> env = new HashMap<>();
        env.put(GitEnvs.FLOW_GIT_SOURCE.name(), GitSource.UNDEFINED_SSH.name());
        env.put(GitEnvs.FLOW_GIT_URL.name(), GITHUB_TEST_REPO_SSH);
        env.put(GitEnvs.FLOW_GIT_SSH_PRIVATE_KEY.name(), getResourceContent("ssh_private_key"));
        envService.save(flow, env, false);

        // when: manual start job
        CountDownLatch latch = new CountDownLatch(1);
        ObjectWrapper<Job> wrapper = new ObjectWrapper<>();

        Map<String, String> envs = EnvUtil.build(GitEnvs.FLOW_GIT_BRANCH.name(), "master");
        jobService.createWithYmlLoad(
            flow.getPath(), JobCategory.MANUAL, envs, currentUser.get(), job -> {
                latch.countDown();
                wrapper.setInstance(job);
            }
        );

        latch.await(60, TimeUnit.SECONDS);

        // then: verify job
        Job created = wrapper.getInstance();
        Assert.assertNotNull(created);
        Assert.assertEquals(JobStatus.FAILURE, created.getStatus());

        // check git commit info
        Assert.assertNotNull(created.getEnv(GitEnvs.FLOW_GIT_COMMIT_ID));
        Assert.assertNotNull(created.getEnv(GitEnvs.FLOW_GIT_AUTHOR));
        Assert.assertNotNull(created.getEnv(GitEnvs.FLOW_GIT_CHANGELOG));
        Assert.assertNotNull(created.getEnv(GitEnvs.FLOW_GIT_BRANCH));
    }

}
