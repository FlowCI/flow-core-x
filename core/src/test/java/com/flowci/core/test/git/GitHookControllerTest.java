/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.test.git;

import com.flowci.common.helper.StringHelper;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.JobCreatedEvent;
import com.flowci.core.test.MockLoggedInScenario;
import com.flowci.core.test.MockMvcHelper;
import com.flowci.core.test.flow.FlowMockHelper;
import com.flowci.domain.ObjectWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * @author yang
 */
public class GitHookControllerTest extends MockLoggedInScenario {

    @Autowired
    private FlowMockHelper flowMockHelper;

    @Autowired
    private MockMvcHelper mockMvcHelper;

    @Test
    void should_start_job_from_github_push_event() throws Exception {
        String yml = StringHelper.toString(load("flow.yml"));
        flowMockHelper.create("github-test", yml);
        String payload = StringHelper.toString(load("github/webhook_push.json"));

        CountDownLatch waitForJobCreated = new CountDownLatch(1);
        ObjectWrapper<Job> jobCreated = new ObjectWrapper<>();
        addEventListener((ApplicationListener<JobCreatedEvent>) event -> {
            jobCreated.setValue(event.getJob());
            waitForJobCreated.countDown();
        });

        mockMvcHelper.expectSuccessAndReturnString(
            post("/webhooks/github-test")
                .header("X-GitHub-Event", "push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload));

        assertTrue(waitForJobCreated.await(10, TimeUnit.SECONDS));
        assertNotNull(jobCreated.getValue());
    }

    @Test
    void should_not_start_job_form_github_push_event_since_branch_not_match() throws Exception {
        String yml = StringHelper.toString(load("flow-with-condition-dev-branch.yml"));
        flowMockHelper.create("github-test", yml);
        String payload = StringHelper.toString(load("github/webhook_push.json"));

        CountDownLatch waitForJobCreated = new CountDownLatch(1);
        ObjectWrapper<Job> jobCreated = new ObjectWrapper<>();
        addEventListener((ApplicationListener<JobCreatedEvent>) event -> {
            jobCreated.setValue(event.getJob());
            waitForJobCreated.countDown();
        });

        mockMvcHelper.expectSuccessAndReturnString(
            post("/webhooks/github-test")
                .header("X-GitHub-Event", "push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload));

        assertFalse(waitForJobCreated.await(1, TimeUnit.SECONDS));
        assertNull(jobCreated.getValue());
    }
}
