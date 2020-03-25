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

import com.flowci.core.job.domain.Job;
import com.flowci.core.plugin.domain.*;
import com.flowci.core.plugin.service.PluginService;
import com.flowci.domain.*;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.NotAvailableException;
import com.flowci.tree.Node;
import com.flowci.tree.StepNode;
import com.flowci.util.StringHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author yang
 */
@Repository
public class CmdManagerImpl implements CmdManager {

    @Autowired
    private PluginService pluginService;

    @Override
    public CmdId createId(Job job, StepNode node) {
        return new CmdId(job.getId(), node.getPath().getPathInStr());
    }

    @Override
    public CmdIn createShellCmd(Job job, StepNode node) {
        // node envs has top priority;
        Vars<String> inputs = new StringVars()
                .merge(job.getContext())
                .merge(node.getEnvironments());

        // create cmd based on plugin
        CmdIn cmd = new CmdIn(createId(job, node).toString(), CmdType.SHELL);
        cmd.setInputs(inputs);
        cmd.setDocker(node.getDocker());
        cmd.setWorkDir(job.getFlowId()); // default work dir is {agent dir}/{flow id}

        cmd.addScript(node.getScript());
        cmd.addEnvFilters(node.getExports());

        if (node.hasPlugin()) {
            setPlugin(node.getPlugin(), cmd);
        }

        // set node allow failure as top priority
        if (node.isAllowFailure() != cmd.isAllowFailure()) {
            cmd.setAllowFailure(node.isAllowFailure());
        }

        return cmd;
    }

    @Override
    public CmdIn createKillCmd() {
        return new CmdIn(UUID.randomUUID().toString(), CmdType.KILL);
    }

    private void setPlugin(String name, CmdIn cmd) {
        Plugin plugin = pluginService.get(name);
        verifyPluginInput(cmd.getInputs(), plugin);

        cmd.setPlugin(name);
        cmd.setAllowFailure(plugin.isAllowFailure());
        cmd.addEnvFilters(plugin.getExports());

        PluginBody body = plugin.getBody();

        if (body instanceof ScriptBody) {
            String script = ((ScriptBody) body).getScript();
            cmd.addScript(script);
            return;
        }

        if (body instanceof ParentBody) {
            ParentBody parentData = (ParentBody) body;
            Plugin parent = pluginService.get(parentData.getName());

            if (!(parent.getBody() instanceof ScriptBody)) {
                throw new NotAvailableException("Script not found on parent plugin");
            }

            String scriptFromParent = ((ScriptBody) parent.getBody()).getScript();
            cmd.addInputs(parentData.getEnvs());
            cmd.addScript(scriptFromParent);

            verifyPluginInput(cmd.getInputs(), parent);
        }
    }

    private static void verifyPluginInput(Vars<String> context, Plugin plugin) {
        for (Input input : plugin.getInputs()) {
            String value = context.get(input.getName());

            // setup plugin default value to context
            if (!StringHelper.hasValue(value) && input.hasDefaultValue()) {
                context.put(input.getName(), input.getValue());
                continue;
            }

            // verify value from context
            if (!input.verify(value)) {
                throw new ArgumentException(
                        "The illegal input {0} for plugin {1}", input.getName(), plugin.getName());
            }
        }
    }
}
