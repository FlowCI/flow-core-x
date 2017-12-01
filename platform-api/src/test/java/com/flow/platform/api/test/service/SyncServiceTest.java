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
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.flow.platform.api.domain.sync.SyncEvent;
import com.flow.platform.api.domain.sync.SyncRepo;
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
import com.flow.platform.util.git.JGitUtil;
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
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
@FixMethodOrder(value = MethodSorters.JVM)
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

        stubFor(post(urlEqualTo("/cmd/queue/send?priority=10&retry=5"))
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
        String gitUrl = "http://localhost/git/hello.git";
        SyncRepo repo = new SyncRepo(JGitUtil.getRepoNameFromGitUrl(gitUrl), "v1.0");
        SyncEvent createEvent = new SyncEvent(gitUrl, repo, SyncType.CREATE);

        String script = "git init hello[v1.0]\n"
            + "cd hello[v1.0]\n"
            + "git pull http://localhost/git/hello.git --tags\n"
            + "git checkout v1.0";
        Assert.assertEquals(script, createEvent.toScript());

        SyncEvent deleteEvent = new SyncEvent(gitUrl, repo, SyncType.DELETE);
        Assert.assertEquals("rm -r -f hello[v1.0]", deleteEvent.toScript());

        SyncEvent listEvent = new SyncEvent(null, null, SyncType.LIST);
        Assert.assertEquals("export FLOW_SYNC_LIST=\"$(ls)\"", listEvent.toScript());
    }

    @Test
    public void should_add_sync_event_been_added_for_agent() throws Throwable {
        // given:
        AgentPath firstAgent = agents.get(0);
        AgentPath secondAgent = agents.get(1);

        syncService.register(firstAgent);
        syncService.register(secondAgent);

        // when: put sync event to service
        syncService.put(new SyncEvent("http://127.0.0.1/git/hello.git", new SyncRepo("hello", "v1.0"), SyncType.CREATE));
        syncService.put(new SyncEvent("http://127.0.0.1/git/flow.git", new SyncRepo("flow", "v1.0"), SyncType.CREATE));

        // then: two events should be in the agent sync queue, include list
        Assert.assertEquals(2, syncService.get(firstAgent).getQueue().size());
        Assert.assertEquals(2, syncService.get(secondAgent).getQueue().size());
    }

    @Test
    public void should_batch_add_sync_event_agent() throws Throwable {
        // given:
        AgentPath firstAgent = agents.get(0);
        AgentPath secondAgent = agents.get(1);

        syncService.register(firstAgent);
        syncService.register(secondAgent);

        List<SyncEvent> events = ImmutableList.of(
            new SyncEvent("http://127.0.0.1/git/hello.git", new SyncRepo("hello", "v1.0"), SyncType.CREATE),
            new SyncEvent("http://127.0.0.1/git/flow.git", new SyncRepo("flow", "v1.0"), SyncType.CREATE)
        );

        // when: put sync event list to service without clean event queue
        syncService.put(events, false);

        // then: two events should be in the agent sync queue, include list
        Assert.assertEquals(2, syncService.get(firstAgent).getQueue().size());
        Assert.assertEquals(2, syncService.get(secondAgent).getQueue().size());

        // when: put sync event list to service again
        syncService.put(events, false);

        // then:
        Assert.assertEquals(4, syncService.get(firstAgent).getQueue().size());
        Assert.assertEquals(4, syncService.get(secondAgent).getQueue().size());

        // when: put sync event list to service again with clean current queue
        syncService.put(events, true);

        // then: the event queue should clean
        Assert.assertEquals(2, syncService.get(firstAgent).getQueue().size());
        Assert.assertEquals(2, syncService.get(secondAgent).getQueue().size());
    }

    @Test
    public void should_init_sync_event_when_agent_registered() throws Throwable {
        // given: copy exist git to workspace
        ClassLoader classLoader = TestBase.class.getClassLoader();
        URL resource = classLoader.getResource("hello.git");
        File path = new File(resource.getFile());
        FileUtils.copyDirectoryToDirectory(path, gitWorkspace.toFile());

        // when: register agent to sync service
        syncService.register(agents.get(0));
        syncService.register(agents.get(1));

        // then: verify the sync event been initialized into both agents

        // check sync queue for first agent
        PlatformQueue<PriorityMessage> queue = syncService.get(agents.get(0)).getQueue();
        Assert.assertEquals(1, queue.size());

        SyncEvent createEvent = SyncEvent.parse(queue.dequeue().getBody(), SyncEvent.class);
        Assert.assertEquals("http://localhost:8080/git/hello.git", createEvent.getGitUrl());
        Assert.assertEquals("v1.0", createEvent.getRepo().getTag());
        Assert.assertEquals(SyncType.CREATE, createEvent.getSyncType());

        // check sync queue for second agent
        queue = syncService.get(agents.get(1)).getQueue();
        Assert.assertEquals(1, queue.size());

        createEvent = SyncEvent.parse(queue.dequeue().getBody(), SyncEvent.class);
        Assert.assertEquals("http://localhost:8080/git/hello.git", createEvent.getGitUrl());
        Assert.assertEquals("v1.0", createEvent.getRepo().getTag());
        Assert.assertEquals(SyncType.CREATE, createEvent.getSyncType());
    }

    @Test
    public void should_execute_sync_event_callback() throws Throwable {
        // given: copy exist git to workspace
        ClassLoader classLoader = TestBase.class.getClassLoader();
        URL resource = classLoader.getResource("hello.git");
        File path = new File(resource.getFile());
        FileUtils.copyDirectoryToDirectory(path, gitWorkspace.toFile());

        // and: register agent to sync service
        AgentPath agent = agents.get(0);
        syncService.register(agent);
        Assert.assertEquals(1, syncService.get(agent).getQueue().size());

        // when: execute sync task
        syncService.syncTask();

        // then: the create session cmd should be send
        CountMatchingStrategy strategy = new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO, 1);
        verify(strategy, postRequestedFor(urlEqualTo("/cmd/queue/send?priority=10&retry=5")));

        // when: mock create session cmd been callback
        Cmd mockSessionCallback = new Cmd(agent.getZone(), agent.getName(), CmdType.CREATE_SESSION, null);
        mockSessionCallback.setStatus(CmdStatus.SENT);
        mockSessionCallback.setSessionId(createSessionCmdResponse.getSessionId());
        syncService.onCallback(mockSessionCallback);

        // then: send run shell cmd for sync event and sync task queue size not changed
        strategy = new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO, 1);
        verify(strategy, postRequestedFor(urlEqualTo("/cmd/send")));
        Assert.assertEquals(2, syncService.getSyncTask(agent).getSyncQueue().size());

        // when: mock cmd been executed
        Cmd mockRunShellSuccess = new Cmd(agent.getZone(), agent.getName(), CmdType.RUN_SHELL, "git pull xxx");
        mockRunShellSuccess.setSessionId(mockSessionCallback.getSessionId());
        mockRunShellSuccess.setStatus(CmdStatus.LOGGED);
        mockRunShellSuccess.setCmdResult(new CmdResult(0));
        syncService.onCallback(mockRunShellSuccess);

        // then: should send list repos cmd and sync task queue size should be 1
        strategy = new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO, 2);
        verify(strategy, postRequestedFor(urlEqualTo("/cmd/send")));
        Assert.assertEquals(1, syncService.getSyncTask(agent).getSyncQueue().size());

        // when: mock list cmd been executed
        Cmd mockListRepoSuccess =
            new Cmd(agent.getZone(), agent.getName(), CmdType.RUN_SHELL, "export FLOW_SYNC_LIST=$(ls)");
        mockListRepoSuccess.setSessionId(mockSessionCallback.getSessionId());
        mockListRepoSuccess.setStatus(CmdStatus.LOGGED);
        mockListRepoSuccess.setCmdResult(new CmdResult(0));
        mockListRepoSuccess.getCmdResult().getOutput().put(SyncEvent.FLOW_SYNC_LIST, "A[v1]\nB[v2]");
        syncService.onCallback(mockListRepoSuccess);

        // then: agent repo list size should be 2
        Assert.assertEquals(2, syncService.get(agent).getRepos().size());
        Assert.assertEquals(new SyncRepo("A", "v1"), syncService.get(agent).getRepos().get(0));
        Assert.assertEquals(new SyncRepo("B", "v2"), syncService.get(agent).getRepos().get(1));

        // then: should send delete session cmd and sync task queue size should be zero
        strategy = new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO, 3);
        verify(strategy, postRequestedFor(urlEqualTo("/cmd/send")));
        Assert.assertEquals(0, syncService.getSyncTask(agent).getSyncQueue().size());

        // when: mock delete session cmd
        Cmd mockDeleteSession = new Cmd(agent.getZone(), agent.getName(), CmdType.DELETE_SESSION, null);
        mockDeleteSession.setSessionId(mockRunShellSuccess.getSessionId());
        mockDeleteSession.setStatus(CmdStatus.SENT);
        syncService.onCallback(mockDeleteSession);

        // then: sync task of agent should be deleted
        Assert.assertNull(syncService.getSyncTask(agent));
    }

    @Test
    public void should_remove_sync_task_if_create_session_failure() throws Throwable {
        // given: remove stub url
        wireMockRule.resetAll();

        // and copy exist git to workspace
        ClassLoader classLoader = TestBase.class.getClassLoader();
        URL resource = classLoader.getResource("hello.git");
        File path = new File(resource.getFile());
        FileUtils.copyDirectoryToDirectory(path, gitWorkspace.toFile());

        // and: register agent to sync service
        AgentPath agent = agents.get(0);
        syncService.register(agent);

        // when: execute sync task
        syncService.syncTask();

        // then: sync task for agent should be removed
        Assert.assertNull(syncService.getSyncTask(agent));
    }

    @Test
    public void should_remove_sync_task_if_create_session_failure_on_callback() throws Throwable {
        // given: copy exist git to workspace
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
        verify(strategy, postRequestedFor(urlEqualTo("/cmd/queue/send?priority=10&retry=5")));

        // when: mock create session failure
        Cmd mockSessionCallback = new Cmd(agent.getZone(), agent.getName(), CmdType.CREATE_SESSION, null);
        mockSessionCallback.setStatus(CmdStatus.EXCEPTION);
        mockSessionCallback.setSessionId(createSessionCmdResponse.getSessionId());
        syncService.onCallback(mockSessionCallback);

        // then: sync task for agent should be removed
        Assert.assertNull(syncService.getSyncTask(agent));
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
