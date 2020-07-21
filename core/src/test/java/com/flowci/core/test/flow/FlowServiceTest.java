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
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.domain.http.ResponseMessage;
import com.flowci.core.flow.domain.ConfirmOption;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Flow.Status;
import com.flowci.core.flow.event.GitTestEvent;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.GitService;
import com.flowci.core.secret.domain.AuthSecret;
import com.flowci.core.secret.service.SecretService;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.SimpleKeyPair;
import com.flowci.domain.VarValue;
import com.flowci.domain.Vars;
import com.flowci.exception.ArgumentException;
import com.flowci.util.StringHelper;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author yang
 */
@FixMethodOrder(value = MethodSorters.JVM)
public class FlowServiceTest extends SpringScenario {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FlowService flowService;

    @Autowired
    private GitService gitService;

    @Autowired
    private String serverUrl;

    @Autowired
    private SecretService secretService;

    private String defaultYml;

    @Before
    public void login() throws IOException {
        mockLogin();
        defaultYml = StringHelper.toString(load("flow.yml"));
    }

    @Test
    public void should_create_with_default_vars() {
        final Flow flow = flowService.create("vars-test");
        should_has_db_info(flow);

        final Vars<VarValue> vars = flow.getLocally();
        Assert.assertEquals(2, vars.size());

        VarValue nameVar = vars.get(Variables.Flow.Name);
        Assert.assertEquals(flow.getName(), nameVar.getData());
        Assert.assertFalse(nameVar.isEditable());

        VarValue webhookVar = vars.get(Variables.Flow.Webhook);
        Assert.assertEquals(serverUrl + "/webhooks/" + flow.getName(), webhookVar.getData());
        Assert.assertFalse(webhookVar.isEditable());
    }

    @Test
    public void should_list_all_flows_by_user_id() {
        ConfirmOption confirmOption = new ConfirmOption().setYaml(defaultYml);

        Flow first = flowService.create("test-1");
        flowService.confirm(first.getName(), confirmOption);

        Flow second = flowService.create("test-2");
        flowService.confirm(second.getName(), confirmOption);

        List<Flow> list = flowService.list(Status.CONFIRMED);
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(first, list.get(0));
        Assert.assertEquals(second, list.get(1));
    }

    @Test
    public void should_create_and_confirm_flow() {
        // when: create flow
        String name = "hello";
        flowService.create(name);

        // then: flow with pending status
        Flow created = flowService.get(name);
        Assert.assertNotNull(created);
        Assert.assertEquals(Status.PENDING, created.getStatus());

        // when: confirm the flow
        ConfirmOption option = new ConfirmOption()
                .setYaml(defaultYml)
                .setGitUrl("git@github.com:FlowCI/docs.git")
                .setSecret("ssh-ras-credential");
        flowService.confirm(name, option);

        // then: flow should be with confirmed status
        Flow confirmed = flowService.get(name);
        Assert.assertNotNull(confirmed);
        Assert.assertEquals(Status.CONFIRMED, confirmed.getStatus());

        // then:
        List<Flow> flows = flowService.list(Status.CONFIRMED);
        Assert.assertEquals(1, flows.size());
    }

    @Test
    public void should_list_flow_by_credential_name() {
        // init:
        String secretName = "flow-ssh-ras-name";
        secretService.createRSA(secretName);

        Flow flow = flowService.create("hello");
        flowService.confirm(flow.getName(), new ConfirmOption().setYaml(defaultYml).setSecret(secretName));

        Vars<VarValue> variables = flowService.get(flow.getName()).getLocally();
        Assert.assertEquals(secretName, variables.get(Variables.Flow.GitCredential).getData());

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
        flowService.create(name);
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
        Flow flow = flowService.create("git-test");
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
        gitService.testConn(flow, gitUrl, privateKey);

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

        Flow flow = flowService.create("git-test");
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
        gitService.testConn(flow, gitUrl, mocked.getName());

        countDown.await(30, TimeUnit.SECONDS);
        Assert.assertTrue(branches.size() >= 1);
    }
}
