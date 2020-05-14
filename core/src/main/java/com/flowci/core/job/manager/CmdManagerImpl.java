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

import com.flowci.core.common.domain.Variables;
import com.flowci.core.job.domain.ExecutedCmd;
import com.flowci.core.job.domain.Job;
import com.flowci.core.plugin.domain.ParentBody;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.domain.PluginBody;
import com.flowci.core.plugin.domain.ScriptBody;
import com.flowci.core.plugin.service.PluginService;
import com.flowci.domain.CmdIn;
import com.flowci.domain.CmdType;
import com.flowci.domain.Vars;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.NotAvailableException;
import com.flowci.tree.StepNode;
import com.flowci.util.ObjectsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * @author yang
 */
@Repository
public class CmdManagerImpl implements CmdManager {

    @Autowired
    private PluginService pluginService;

    @Override
    public CmdIn createShellCmd(Job job, StepNode node, ExecutedCmd cmd) {
        CmdIn in = new CmdIn(cmd.getId(), CmdType.SHELL);
        in.setFlowId(cmd.getFlowId()); // default work dir is {agent dir}/{flow id}
        in.setJobId(cmd.getJobId());
        in.setBuildNumber(cmd.getBuildNumber());
        in.setAfter(cmd.isAfter());

        // load setting from yaml StepNode
        in.setNodePath(node.getPathAsString());
        in.setDocker(node.getDocker());
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
        return new CmdIn(UUID.randomUUID().toString(), CmdType.KILL);
    }

    private void setPlugin(String name, CmdIn cmd) {
        Plugin plugin = pluginService.get(name);
        Optional<String> validate = plugin.verifyInput(cmd.getInputs());
        if (validate.isPresent()) {
            throw new ArgumentException("The illegal input {0} for plugin {1}", validate.get(), plugin.getName());
        }

        cmd.setPlugin(name);
        cmd.setAllowFailure(plugin.isAllowFailure());
        cmd.addEnvFilters(plugin.getExports());

        // apply docker from plugin if it's specified
        ObjectsHelper.ifNotNull(plugin.getDocker(), (docker) -> {
            cmd.setDocker(plugin.getDocker());
        });

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

            // apply docker option from parent if not specified
            if (!cmd.hasDockerOption()) {
                cmd.setDocker(parent.getDocker());
            }

            validate = parent.verifyInput(cmd.getInputs());
            if (validate.isPresent()) {
                throw new ArgumentException("The illegal input {0} for plugin {1}", validate.get(), parent.getName());
            }
        }
    }

    private static boolean isDockerEnabled(Vars<String> input) {
        String val = input.get(Variables.Step.DockerEnabled, "true");
        return Boolean.parseBoolean(val);
    }
}
