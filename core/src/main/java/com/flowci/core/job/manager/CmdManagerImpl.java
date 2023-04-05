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

import com.flowci.core.agent.domain.ShellIn;
import com.flowci.core.agent.domain.ShellKill;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Step;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.event.GetPluginAndVerifySetContext;
import com.flowci.core.plugin.event.GetPluginEvent;
import com.flowci.domain.tree.DockerOption;
import com.flowci.domain.Vars;
import com.flowci.exception.StatusException;
import com.flowci.parser.v1.*;
import com.flowci.domain.tree.NodePath;
import com.flowci.util.ObjectsHelper;
import com.flowci.util.StringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author yang
 */
@Component
public class CmdManagerImpl implements CmdManager {

    @Autowired
    private SpringEventManager eventManager;

    @Override
    public ShellIn createShellCmd(Job job, Step step, Node node) {
        if (node instanceof ParallelStepNode) {
            throw new StatusException("Illegal step node type, must be regular step");
        }

        RegularStepNode r = (RegularStepNode) node;
        ShellIn in = new ShellIn()
                .setId(step.getId())
                .setFlowId(job.getFlowId())
                .setJobId(job.getId())
                .setAllowFailure(r.isAllowFailure())
                .setDockers(ObjectsHelper.copy(r.fetchDockerOptions()))
                .setBash(r.fetchBash())
                .setPwsh(r.fetchPwsh())
                .setEnvFilters(r.fetchFilters())
                .setInputs(r.fetchEnvs().merge(job.getContext(), false))
                .setTimeout(r.fetchTimeout(job.getTimeout()))
                .setRetry(r.fetchRetry(0))
                .setSecrets(r.getSecrets())
                .setConfigs(r.getConfigs())
                .setCache(r.getCache());

        if (r.hasPlugin()) {
            setPlugin(r.getPlugin(), in);
        }

        // set node allow failure as top priority
        if (r.isAllowFailure() != in.isAllowFailure()) {
            in.setAllowFailure(r.isAllowFailure());
        }

        // auto create default container name
        for (DockerOption option : in.getDockers()) {
            if (!option.hasName()) {
                option.setName(getDefaultContainerName(r));
            }
        }

        if (!isDockerEnabled(job.getContext())) {
            in.getDockers().clear();
        }

        return in;
    }

    @Override
    public ShellKill createKillCmd() {
        return new ShellKill();
    }

    private String getDefaultContainerName(RegularStepNode node) {
        NodePath path = node.getPath();
        String stepStr = path.getNodePathWithoutSpace().replace(NodePath.PathSeparator, "-");
        return StringHelper.escapeNumber(String.format("%s-%s", stepStr, StringHelper.randomString(5)));
    }

    private void setPlugin(String name, ShellIn cmd) {
        GetPluginEvent event = eventManager.publish(new GetPluginAndVerifySetContext(this, name, cmd.getInputs()));
        if (event.hasError()) {
            throw event.getError();
        }

        Plugin plugin = event.getFetched();
        cmd.setPlugin(name);
        cmd.setAllowFailure(plugin.getMeta().isAllowFailure());
        cmd.addEnvFilters(plugin.getMeta().getExports());
        cmd.addScript(plugin.getMeta().getBash(), ShellIn.ShellType.Bash);
        cmd.addScript(plugin.getMeta().getPwsh(), ShellIn.ShellType.PowerShell);

        // apply docker from plugin as run time if it's specified
        ObjectsHelper.ifNotNull(plugin.getMeta().getDocker(), (docker) -> {
            Iterator<DockerOption> iterator = cmd.getDockers().iterator();
            while (iterator.hasNext()) {
                DockerOption option = iterator.next();
                if (option.isRuntime()) {
                    iterator.remove();
                    break;
                }
            }
            cmd.getDockers().add(plugin.getMeta().getDocker());
        });
    }

    private static boolean isDockerEnabled(Vars<String> input) {
        String val = input.get(Variables.Step.DockerEnabled, "true");
        return Boolean.parseBoolean(val);
    }
}
