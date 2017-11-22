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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.flow.platform.api.domain.sync.SyncEvent;
import com.flow.platform.api.domain.sync.SyncType;
import com.flow.platform.api.service.SyncService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.queue.PriorityMessage;
import com.flow.platform.domain.AgentPath;
import com.flow.platform.domain.Cmd;
import com.flow.platform.domain.CmdResult;
import com.flow.platform.domain.CmdStatus;
import com.flow.platform.domain.CmdType;
import com.flow.platform.queue.PlatformQueue;
import com.github.tomakehurst.wiremock.client.CountMatchingStrategy;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class SyncServiceTest extends TestBase {

    @Autowired
    private SyncService syncService;

    @Autowired
    private Path gitWorkspace;

    private List<AgentPath> agents;

    private Cmd createSessionCmdResponse;

    @Before
    public void init() {
        agents = ImmutableList.of(new AgentPath("default", "first"), new AgentPath("default", "second"));

        createSessionCmdResponse = new Cmd();
        createSessionCmdResponse.setId(UUID.randomUUID().toString());
        createSessionCmdResponse.setSessionId(UUID.randomUUID().toString());

        stubFor(post(urlEqualTo("/cmd/queue/send?priority=1&retry=5"))
            .willReturn(aResponse()
                .withBody(createSessionCmdResponse.toJson())));


        Cmd mockResponse = new Cmd();
        mockResponse.setId(UUID.randomUUID().toString());
        mockResponse.setSessionId(createSessionCmdResponse.getSessionId());

        stubFor(post(urlEqualTo("/cmd/send"))
            .willReturn(aResponse().withBody(mockResponse.toJson())));
    }

    @Test
    public void should_convert_sync_event_to_script() throws Throwable {
        SyncEvent createEvent = new SyncEvent("http://localhost/git/hello.git", "v1.0", SyncType.CREATE);
        String script = "git init hello\ncd hello\ngit pull http://localhost/git/hello.git --tags\ngit checkout v1.0";
        Assert.assertEquals(script, createEvent.toScript());

        SyncEvent deleteEvent = new SyncEvent("http://localhost/git/hello.git", "v1.0", SyncType.DELETE);
        Assert.assertEquals("rm -r -f hello", deleteEvent.toScript());
    }

    @Test
    public void should_sync_event_been_added_for_agent() throws Throwable {
        // given:
        AgentPath firstAgent = agents.get(0);
        AgentPath secondAgent = agents.get(1);

        syncService.register(firstAgent);
        syncService.register(secondAgent);

        // when: put sync event to service
        syncService.put(new SyncEvent("http://127.0.0.1/git/hello.git", "v1.0", SyncType.CREATE));
        syncService.put(new SyncEvent("http://127.0.0.1/git/flow.git", "v1.0", SyncType.CREATE));

        // then: two events should be in the agent sync queue
        Assert.assertEquals(2, syncService.getSyncQueue(firstAgent).size());
        Assert.assertEquals(2, syncService.getSyncQueue(secondAgent).size());
    }

    @Test
    public void should_init_sync_event_when_agent_registered() throws Throwable {
        // given: copy exit git to workspace
        ClassLoader classLoader = TestBase.class.getClassLoader();
        URL resource = classLoader.getResource("hello.git");
        File path = new File(resource.getFile());
        FileUtils.copyDirectoryToDirectory(path, gitWorkspace.toFile());

        // when: register agent to sync service
        syncService.register(agents.get(0));
        syncService.register(agents.get(1));

        // then: verify the sync event been initialized into both agents
        PlatformQueue<PriorityMessage> queue = syncService.getSyncQueue(agents.get(0));
        SyncEvent event = SyncEvent.parse(queue.dequeue().getBody(), SyncEvent.class);
        Assert.assertEquals("http://localhost:8080/git/hello.git", event.getGitUrl());
        Assert.assertEquals("v1.0", event.getTag());
        Assert.assertEquals(SyncType.CREATE, event.getSyncType());

        queue = syncService.getSyncQueue(agents.get(1));
        event = SyncEvent.parse(queue.dequeue().getBody(), SyncEvent.class);
        Assert.assertEquals("http://localhost:8080/git/hello.git", event.getGitUrl());
        Assert.assertEquals("v1.0", event.getTag());
        Assert.assertEquals(SyncType.CREATE, event.getSyncType());
    }

    @Test
    public void should_send_create_session_cmd_for_sync_task() throws Throwable {
        // given: copy exit git to workspace
        ClassLoader classLoader = TestBase.class.getClassLoader();
        URL resource = classLoader.getResource("hello.git");
        File path = new File(resource.getFile());
        FileUtils.copyDirectoryToDirectory(path, gitWorkspace.toFile());

        // and: register agent to sync service
        AgentPath agent = agents.get(0);
        syncService.register(agent);

        // when: execute sync task
        syncService.syncTask();

        // then: the create session cmd should be send
        CountMatchingStrategy strategy = new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO, 1);
        verify(strategy, postRequestedFor(urlEqualTo("/cmd/queue/send?priority=1&retry=5")));

        // when: mock create session cmd been callback
        Cmd mockSessionCallback = new Cmd(agent.getZone(), agent.getName(), CmdType.CREATE_SESSION, null);
        mockSessionCallback.setStatus(CmdStatus.SENT);
        mockSessionCallback.setSessionId(createSessionCmdResponse.getSessionId());
        syncService.onCallback(mockSessionCallback);

        // then: send run shell cmd for sync event and sync task queue size not changed
        strategy = new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO, 1);
        verify(strategy, postRequestedFor(urlEqualTo("/cmd/send")));
        Assert.assertEquals(1, syncService.getSyncTask(agent).size());

        // when: mock cmd been executed
        Cmd mockRunShellSuccess = new Cmd(agent.getZone(), agent.getName(), CmdType.RUN_SHELL, "git pull xxx");
        mockRunShellSuccess.setSessionId(mockSessionCallback.getSessionId());
        mockRunShellSuccess.setStatus(CmdStatus.LOGGED);
        mockRunShellSuccess.setCmdResult(new CmdResult(0));
        syncService.onCallback(mockRunShellSuccess);

        // then: should send delete session cmd and sync task queue size should be zero
        strategy = new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO, 2);
        verify(strategy, postRequestedFor(urlEqualTo("/cmd/send")));
        Assert.assertEquals(0, syncService.getSyncTask(agent).size());
    }

    @After
    public void clean() throws IOException {
        File[] files = gitWorkspace.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                FileUtils.forceDelete(file);
            }
        }

        syncService.clean();
    }
}
