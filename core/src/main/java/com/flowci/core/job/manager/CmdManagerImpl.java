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

package com.flowci.core.job.manager;

import com.flowci.core.agent.domain.CmdIn;
import com.flowci.core.agent.domain.ShellKill;
import com.flowci.core.agent.domain.ShellIn;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.job.domain.Step;
import com.flowci.core.job.domain.Job;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.event.GetPluginAndVerifySetContext;
import com.flowci.core.plugin.event.GetPluginEvent;
import com.flowci.domain.Vars;
import com.flowci.tree.StepNode;
import com.flowci.util.ObjectsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Component
public class CmdManagerImpl implements CmdManager {

    @Autowired
    private SpringEventManager eventManager;

    @Override
    public CmdIn createShellCmd(Job job, StepNode node, Step step) {
        ShellIn in = new ShellIn()
                .setId(step.getId())
                .setFlowId(job.getFlowId())
                .setAllowFailure(node.isAllowFailure())
                .setDocker(node.getDocker())
                .setTimeout(job.getTimeout());

        // load setting from yaml StepNode
        in.addScript(node.getScript());
        in.addEnvFilters(node.getExports());
        in.getInputs().merge(job.getContext()).merge(node.getEnvironments());

        if (node.hasPlugin()) {
            setPlugin(node.getPlugin(), in);
        }

        // set node allow failure as top priority
        if (node.isAllowFailure() != in.isAllowFailure()) {
            in.setAllowFailure(node.isAllowFailure());
        }

        if (!isDockerEnabled(job.getContext())) {
            in.setDocker(null);
        }

        return in;
    }

    @Override
    public CmdIn createKillCmd() {
        return new ShellKill();
    }

    private void setPlugin(String name, ShellIn cmd) {
        GetPluginEvent event = eventManager.publish(new GetPluginAndVerifySetContext(this, name, cmd.getInputs()));
        if (event.hasError()) {
            throw event.getError();
        }

        Plugin plugin = event.getFetched();
        cmd.setPlugin(name);
        cmd.setAllowFailure(plugin.isAllowFailure());
        cmd.addEnvFilters(plugin.getExports());
        cmd.addScript(plugin.getScript());

        // apply docker from plugin if it's specified
        ObjectsHelper.ifNotNull(plugin.getDocker(), (docker) -> {
            cmd.setDocker(plugin.getDocker());
        });
    }

    private static boolean isDockerEnabled(Vars<String> input) {
        String val = input.get(Variables.Step.DockerEnabled, "true");
        return Boolean.parseBoolean(val);
    }
}
