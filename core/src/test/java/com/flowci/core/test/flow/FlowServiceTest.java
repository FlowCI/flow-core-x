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

package com.flowci.core.test.flow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.domain.http.ResponseMessage;
import com.flowci.core.flow.domain.CreateOption;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.event.GitTestEvent;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.FlowSettingService;
import com.flowci.core.flow.service.GitConnService;
import com.flowci.core.git.domain.GitCommit;
import com.flowci.core.git.domain.GitPushTrigger;
import com.flowci.core.git.domain.GitTrigger;
import com.flowci.core.git.domain.GitUser;
import com.flowci.core.git.event.GitHookEvent;
import com.flowci.core.job.event.CreateNewJobEvent;
import com.flowci.core.secret.domain.AuthSecret;
import com.flowci.core.secret.service.SecretService;
import com.flowci.core.test.MockLoggedInScenario;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.*;
import com.flowci.exception.ArgumentException;
import com.flowci.util.StringHelper;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author yang
 */
@FixMethodOrder(value = MethodSorters.JVM)
public class FlowServiceTest extends MockLoggedInScenario {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FlowService flowService;

    @Autowired
    private FlowSettingService flowSettingService;

    @Autowired
    private GitConnService gitConnService;

    @Autowired
    private SecretService secretService;

    private String defaultYml;

    @Before
    public void init() throws IOException {
        defaultYml = StringHelper.toString(load("flow.yml"));
    }

    @Test
    public void should_create_with_default_vars() {
        var option = new CreateOption();
        option.setRawYaml(StringHelper.toBase64(defaultYml));

        Flow flow = flowService.create("vars-test", option);
        shouldHasCreatedAtAndCreatedBy(flow);

        Vars<VarValue> vars = flow.getVars();
        Assert.assertEquals(1, vars.size());

        VarValue nameVar = vars.get(Variables.Flow.Name);
        Assert.assertEquals(flow.getName(), nameVar.getData());
        Assert.assertFalse(nameVar.isEditable());
    }

    @Test
    public void should_list_flow_by_credential_name() {
        // init:
        String secretName = "flow-ssh-ras-name";
        secretService.createRSA(secretName);

        var option = new CreateOption();
        option.setRawYaml(StringHelper.toBase64(defaultYml));
        Flow flow = flowService.create("hello", option);

        var vars = new HashMap<String, VarValue>();
        vars.put(Variables.Git.SECRET, VarValue.of(secretName, VarType.STRING));
        flowSettingService.add(flow, vars);

        Vars<VarValue> variables = flowService.get(flow.getName()).getVars();
        Assert.assertEquals(secretName, variables.get(Variables.Git.SECRET).getData());

        // when:
        List<Flow> flows = flowService.listByCredential(secretName);
        Assert.assertNotNull(flows);
        Assert.assertEquals(1, flows.size());

        // then:
        Assert.assertEquals(flow.getName(), flows.get(0).getName());
    }

    @Test(expected = ArgumentException.class)
    public void should_throw_exception_if_flow_name_is_invalid_when_create() {
        String name = "hello.world";
        flowService.create(name, new CreateOption());
    }

    @Test
    public void should_start_job_with_condition() throws IOException, InterruptedException {
        String rawYaml = StringHelper.toString(load("flow-with-condition.yml"));

        CreateOption option = new CreateOption();
        option.setRawYaml(StringHelper.toBase64(rawYaml));

        Flow flow = flowService.create("githook", option);

        GitPushTrigger trigger = new GitPushTrigger();
        trigger.setEvent(GitTrigger.GitEvent.PUSH);
        trigger.setSource(GitSource.GITEE);
        trigger.setRef("master");
        trigger.setCommits(List.of(new GitCommit().setId("112233").setMessage("dummy commit")));
        trigger.setSender(new GitUser().setEmail("dummy@sender.com"));

        CountDownLatch c = new CountDownLatch(1);
        addEventListener((ApplicationListener<CreateNewJobEvent>) e -> {
            Assert.assertEquals(flow, e.getFlow());
            c.countDown();
        });

        GitHookEvent e = new GitHookEvent(this, flow.getName(), trigger);
        multicastEvent(e);

        Assert.assertTrue(c.await(5, TimeUnit.SECONDS));
    }

    @Ignore
    @Test
    public void should_list_remote_branches_via_ssh_rsa() throws IOException, InterruptedException {
        // init: load private key
        TypeReference<ResponseMessage<SimpleKeyPair>> keyPairResponseType =
                new TypeReference<ResponseMessage<SimpleKeyPair>>() {
                };

        ResponseMessage<SimpleKeyPair> r = objectMapper.readValue(load("rsa-test.json"), keyPairResponseType);

        // given:
        Flow flow = flowService.create("git-test", new CreateOption());
        CountDownLatch countDown = new CountDownLatch(2);
        List<String> branches = new LinkedList<>();

        addEventListener((ApplicationListener<GitTestEvent>) event -> {
            if (!event.getFlowId().equals(flow.getId())) {
                return;
            }

            if (event.getStatus() == GitTestEvent.Status.FETCHING) {
                countDown.countDown();
            }

            if (event.getStatus() == GitTestEvent.Status.DONE) {
                countDown.countDown();
                branches.addAll(event.getBranches());
            }
        });

        // when:
        String gitUrl = "git@github.com:FlowCI/docs.git";
        String privateKey = r.getData().getPrivateKey();
        gitConnService.testConn(flow, gitUrl, privateKey);

        // then:
        countDown.await(30, TimeUnit.SECONDS);
        Assert.assertTrue(branches.size() >= 1);
    }

    @Ignore
    @Test
    public void should_list_remote_branches_via_http_with_credential() throws InterruptedException {
        // given:
        String credentialName = "test-auth-c";
        AuthSecret mocked = secretService.createAuth(credentialName, SimpleAuthPair.of("xxx", "xxx"));

        Flow flow = flowService.create("git-test", new CreateOption());
        CountDownLatch countDown = new CountDownLatch(2);
        List<String> branches = new LinkedList<>();

        addEventListener((ApplicationListener<GitTestEvent>) event -> {
            if (!event.getFlowId().equals(flow.getId())) {
                return;
            }

            if (event.getStatus() == GitTestEvent.Status.FETCHING) {
                countDown.countDown();
            }

            if (event.getStatus() == GitTestEvent.Status.DONE) {
                countDown.countDown();
                branches.addAll(event.getBranches());
            }
        });

        String gitUrl = "https://xxxx";
        gitConnService.testConn(flow, gitUrl, mocked.getName());

        countDown.await(30, TimeUnit.SECONDS);
        Assert.assertTrue(branches.size() >= 1);
    }
}
