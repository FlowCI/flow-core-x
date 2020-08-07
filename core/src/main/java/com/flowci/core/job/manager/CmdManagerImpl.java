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
import com.flowci.core.agent.domain.ShellIn;
import com.flowci.core.agent.domain.ShellKill;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Step;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.event.GetPluginAndVerifySetContext;
import com.flowci.core.plugin.event.GetPluginEvent;
import com.flowci.domain.DockerOption;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import com.flowci.tree.Node;
import com.flowci.tree.NodePath;
import com.flowci.tree.NodeTree;
import com.flowci.tree.StepNode;
import com.flowci.util.ObjectsHelper;
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
    public CmdIn createShellCmd(Job job, Step step, NodeTree tree) {
        StepNode node = tree.get(NodePath.create(step.getNodePath()));

        ShellIn in = new ShellIn()
                .setId(step.getId())
                .setFlowId(job.getFlowId())
                .setJobId(job.getId())
                .setCondition(node.getCondition())
                .setAllowFailure(node.isAllowFailure())
                .setDockers(findDockerOptions(node))
                .setScripts(linkScript(node))
                .setEnvFilters(linkFilters(node))
                .setInputs(linkInputs(node).merge(job.getContext(), false))
                .setTimeout(job.getTimeout());

        if (node.hasPlugin()) {
            setPlugin(node.getPlugin(), in);
        }

        // set node allow failure as top priority
        if (node.isAllowFailure() != in.isAllowFailure()) {
            in.setAllowFailure(node.isAllowFailure());
        }

        if (!isDockerEnabled(job.getContext())) {
            in.getDockers().clear();
        }

        return in;
    }

    @Override
    public CmdIn createKillCmd() {
        return new ShellKill();
    }

    private StringVars linkInputs(Node current) {
        StringVars output = new StringVars();

        if (current.hasParent()) {
            Node parent = current.getParent();
            output.merge(linkInputs(parent));
        }

        output.merge(current.getEnvironments());
        return output;
    }

    private Set<String> linkFilters(StepNode current) {
        Set<String> output = new LinkedHashSet<>();

        if (current.hasParent()) {
            Node parent = current.getParent();
            if (parent instanceof StepNode) {
                output.addAll(linkFilters((StepNode) parent));
            }
        }

        output.addAll(current.getExports());
        return output;
    }

    private List<String> linkScript(StepNode current) {
        List<String> output = new LinkedList<>();

        if (current.hasParent()) {
            Node parent = current.getParent();
            if (parent instanceof StepNode) {
                output.addAll(linkScript((StepNode) parent));
            }
        }

        output.add(current.getScript());
        return output;
    }

    private List<DockerOption> findDockerOptions(Node current) {
        if (current.hasDocker()) {
            return current.getDockers();
        }

        if (current.hasParent()) {
            return findDockerOptions(current.getParent());
        }

        return new LinkedList<>();
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

        // apply docker from plugin as run time if it's specified
        ObjectsHelper.ifNotNull(plugin.getDocker(), (docker) -> {
            Iterator<DockerOption> iterator = cmd.getDockers().iterator();
            while (iterator.hasNext()) {
                DockerOption option = iterator.next();
                if (option.isRuntime()) {
                    iterator.remove();
                    break;
                }
            }
            cmd.getDockers().add(plugin.getDocker());
        });
    }

    private static boolean isDockerEnabled(Vars<String> input) {
        String val = input.get(Variables.Step.DockerEnabled, "true");
        return Boolean.parseBoolean(val);
    }
}
