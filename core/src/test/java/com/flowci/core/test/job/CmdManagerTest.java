/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.test.job;

import com.flowci.common.domain.*;
import com.flowci.core.agent.domain.ShellIn;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.flow.domain.CreateOption;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.YmlService;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Step;
import com.flowci.core.job.manager.CmdManager;
import com.flowci.core.job.service.JobService;
import com.flowci.core.job.service.StepService;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.event.GetPluginEvent;
import com.flowci.core.test.MockLoggedInScenario;
import com.flowci.tree.*;
import com.flowci.common.helper.StringHelper;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.util.List;

import static com.flowci.tree.FlowNode.DEFAULT_ROOT_NAME;
import static org.junit.jupiter.api.Assertions.*;

public class CmdManagerTest extends MockLoggedInScenario {

    @Autowired
    private FlowService flowService;

    @Autowired
    private YmlService ymlService;

    @Autowired
    private JobService jobService;

    @Autowired
    private StepService stepService;

    @Autowired
    private CmdManager cmdManager;

    @MockBean
    private SpringEventManager eventManager;

    @Test
    void should_apply_flow_level_docker_option() throws IOException {
        // given: flow and job
        var yaml = StringHelper.toString(load("flow-with-root-docker.yml"));
        var option = new CreateOption().setRawYaml(StringHelper.toBase64(yaml));
        var flow = flowService.create("hello", option);
        var ymlEntity = ymlService.get(flow.getId());

        Job job = jobService.create(flow, ymlEntity.getList(), Job.Trigger.MANUAL, new StringVars());
        assertNotNull(job);

        FlowNode root = YmlParser.load(yaml);
        NodeTree tree = NodeTree.create(root);

        // when: create first shell cmd
        Node node = tree.get(NodePath.create(DEFAULT_ROOT_NAME, "step-docker"));
        Step step = stepService.get(job.getId(), node.getPath().getPathInStr());

        // then: first step docker should be applied from step level
        ShellIn in = cmdManager.createShellCmd(job, step, node);
        assertNotNull(in.getDockers());
        assertEquals(1, in.getDockers().size());
        assertEquals("step:0.1", in.getDockers().get(0).getImage());

        // when: create second shell cmd
        node = tree.get(NodePath.create(DEFAULT_ROOT_NAME, "flow-docker"));
        step = stepService.get(job.getId(), node.getPath().getPathInStr());

        // then: first step docker should be applied from step level
        in = cmdManager.createShellCmd(job, step, node);
        assertNotNull(in.getDockers());
        assertEquals(1, in.getDockers().size());
        assertEquals("helloworld:0.1", in.getDockers().get(0).getImage());
    }

    @Test
    void should_create_cmd_in_with_default_plugin_value() throws IOException {
        // init: setup mock plugin service
        Plugin plugin = createDummyPlugin();
        GetPluginEvent event = new GetPluginEvent(this, plugin.getName());
        event.setFetched(plugin);
        Mockito.when(eventManager.publish(Mockito.any())).thenReturn(event);

        // given: flow and job
        var yaml = StringHelper.toString(load("flow-with-plugin.yml"));
        var option = new CreateOption().setRawYaml(StringHelper.toBase64(yaml));
        var flow = flowService.create("hello", option);
        var ymlEntity = ymlService.get(flow.getId());

        Job job = jobService.create(flow, ymlEntity.getList(), Job.Trigger.MANUAL, new StringVars());
        assertNotNull(job);

        // when: create shell cmd
        FlowNode root = YmlParser.load(yaml);
        NodeTree tree = NodeTree.create(root);
        Node node = tree.get(NodePath.create(DEFAULT_ROOT_NAME, "plugin-test"));
        Step step = stepService.get(job.getId(), node.getPath().getPathInStr());

        ShellIn cmdIn = cmdManager.createShellCmd(job, step, node);
        assertNotNull(cmdIn);

        // then:
        Vars<String> inputs = cmdIn.getInputs();
        List<String> scripts = cmdIn.getBash();
        assertEquals(2, scripts.size());

        assertEquals("gittest", cmdIn.getPlugin());
        assertEquals("test", inputs.get("GIT_STR_VAL"));
        assertEquals("60", inputs.get("GIT_DEFAULT_VAL"));

        // then: docker option should from step
        assertEquals(1, cmdIn.getDockers().size());
        DockerOption docker = cmdIn.getDockers().get(0);
        assertNotNull(docker);
        assertEquals("ubuntu:19.04", docker.getImage());

        String containerNamePrefix = String.format("%s-%s", DEFAULT_ROOT_NAME, "plugin-test");
        assertTrue(docker.getName().startsWith(containerNamePrefix));
    }

    @Test
    void should_handle_step_in_step() throws IOException {
        // given: flow and job
        var yaml = StringHelper.toString(load("step-in-step.yml"));
        var option = new CreateOption().setRawYaml(StringHelper.toBase64(yaml));
        var flow = flowService.create("hello", option);
        var ymlEntity = ymlService.get(flow.getId());

        Job job = jobService.create(flow, ymlEntity.getList(), Job.Trigger.MANUAL, new StringVars());
        assertNotNull(job);

        // when: create shell cmd
        FlowNode root = YmlParser.load(yaml);
        NodeTree tree = NodeTree.create(root);

        Node step2_1 = tree.get(NodePath.create(DEFAULT_ROOT_NAME, "step2", "step-2-1"));
        Node step2_2 = tree.get(NodePath.create(DEFAULT_ROOT_NAME, "step2", "step-2-2"));

        // then: verify step 2 - 1 cmd
        ShellIn cmdStep2_1 = cmdManager.createShellCmd(job, stepService.get(job.getId(), step2_1.getPath().getPathInStr()), step2_1);
        assertEquals(500, cmdStep2_1.getTimeout());
        assertEquals(2, cmdStep2_1.getRetry());

        // input should be overwritten
        assertEquals("overwrite-parent", cmdStep2_1.getInputs().get("STEP_2"));
        assertEquals("overwrite-parent", cmdStep2_1.getInputs().get("STEP_2"));

        // scripts should be linked
        assertEquals("echo 2", cmdStep2_1.getBash().get(0));
        assertEquals("echo \"step-2-1\"\n", cmdStep2_1.getBash().get(1));

        // docker should from parent step
        assertEquals("ubuntu:18.04", cmdStep2_1.getDockers().get(0).getImage());
        assertEquals("mysql", cmdStep2_1.getDockers().get(1).getImage());

        // then: verify step 2 - 2 cmd
        ShellIn cmdStep2_2 = cmdManager.createShellCmd(job, stepService.get(job.getId(), step2_2.getPath().getPathInStr()), step2_2);
        assertEquals("parent", cmdStep2_2.getInputs().get("STEP_2"));
        assertEquals(1000, cmdStep2_2.getTimeout());
        assertEquals(5, cmdStep2_2.getRetry());

        // scripts should be linked
        assertEquals("echo 2", cmdStep2_2.getBash().get(0));
        assertEquals("echo \"step-2-2\"\n", cmdStep2_2.getBash().get(1));

        // docker should be applied from step2-2
        assertEquals("redis", cmdStep2_2.getDockers().get(0).getImage());
    }

    private Plugin createDummyPlugin() {
        Input intInput = new Input();
        intInput.setName("GIT_DEFAULT_VAL");
        intInput.setValue("60");
        intInput.setType(VarType.INT);
        intInput.setRequired(false);

        Input strInput = new Input();
        strInput.setName("GIT_STR_VAL");
        strInput.setValue("setup git str val");
        strInput.setType(VarType.STRING);
        strInput.setRequired(true);

        DockerOption option = new DockerOption();
        option.setImage("ubuntu:19.04");

        Plugin plugin = new Plugin();
        plugin.setName("gittest");

        Plugin.Meta meta = new Plugin.Meta();
        meta.setInputs(Lists.newArrayList(intInput, strInput));
        meta.setDocker(option);
        meta.setBash("echo ${GIT_DEFAULT_VAL} ${GIT_STR_VAL}");
        plugin.setMeta(meta);

        return plugin;
    }
}
