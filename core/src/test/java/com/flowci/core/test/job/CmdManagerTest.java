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

import com.flowci.core.agent.domain.ShellIn;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.YmlService;
import com.flowci.core.job.domain.Step;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.manager.CmdManager;
import com.flowci.core.job.service.JobService;
import com.flowci.core.job.service.StepService;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.event.GetPluginEvent;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.*;
import com.flowci.tree.*;
import com.flowci.util.StringHelper;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.util.List;

public class CmdManagerTest extends SpringScenario {

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

    @Before
    public void login() {
        mockLogin();
    }

    @Test
    public void should_create_cmd_in_with_default_plugin_value() throws IOException {
        // init: setup mock plugin service
        Plugin plugin = createDummyPlugin();
        GetPluginEvent event = new GetPluginEvent(this, plugin.getName());
        event.setFetched(plugin);
        Mockito.when(eventManager.publish(Mockito.any())).thenReturn(event);

        // given: flow and job
        Flow flow = flowService.create("hello");
        Yml yml = ymlService.saveYml(flow, StringHelper.toString(load("flow-with-plugin.yml")));

        Job job = jobService.create(flow, yml.getRaw(), Job.Trigger.MANUAL, new StringVars());
        Assert.assertNotNull(job);

        // when: create shell cmd
        FlowNode root = YmlParser.load(flow.getName(), yml.getRaw());
        NodeTree tree = NodeTree.create(root);
        StepNode node = tree.get(NodePath.create(flow.getName(), "plugin-test"));
        Step step = stepService.get(job.getId(), node.getPathAsString());
        ShellIn cmdIn = (ShellIn) cmdManager.createShellCmd(job, node, step);
        Assert.assertNotNull(cmdIn);

        // then:
        Vars<String> inputs = cmdIn.getInputs();
        List<String> scripts = cmdIn.getScripts();
        Assert.assertEquals(2, scripts.size());

        Assert.assertEquals("gittest", cmdIn.getPlugin());
        Assert.assertEquals("test", inputs.get("GIT_STR_VAL"));
        Assert.assertEquals("60", inputs.get("GIT_DEFAULT_VAL"));

        // then: docker option should from step
        DockerOption docker = plugin.getDocker();
        Assert.assertNotNull(docker);
        Assert.assertEquals("ubuntu:19.04", docker.getImage());
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
        plugin.setInputs(Lists.newArrayList(intInput, strInput));
        plugin.setDocker(option);
        plugin.setScript("echo ${GIT_DEFAULT_VAL} ${GIT_STR_VAL}");

        return plugin;
    }
}
