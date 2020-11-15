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
import com.flowci.domain.DockerOption;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import com.flowci.tree.*;
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
    public ShellIn createShellCmd(Job job, Step step, NodeTree tree) {
        StepNode node = tree.get(NodePath.create(step.getNodePath()));

        ShellIn in = new ShellIn()
                .setId(step.getId())
                .setFlowId(job.getFlowId())
                .setJobId(job.getId())
                .setAllowFailure(node.isAllowFailure())
                .setDockers(ObjectsHelper.copy(findDockerOptions(node)))
                .setBash(linkScript(node, ShellIn.ShellType.Bash))
                .setPwsh(linkScript(node, ShellIn.ShellType.PowerShell))
                .setEnvFilters(linkFilters(node))
                .setInputs(linkInputs(node).merge(job.getContext(), false))
                .setTimeout(linkTimeout(node, job.getTimeout()))
                .setRetry(linkRetry(node, 0))
                .setCache(linkCache(node));

        if (node.hasPlugin()) {
            setPlugin(node.getPlugin(), in);
        }

        // set node allow failure as top priority
        if (node.isAllowFailure() != in.isAllowFailure()) {
            in.setAllowFailure(node.isAllowFailure());
        }

        // auto create default container name
        for (DockerOption option : in.getDockers()) {
            if (!option.hasName()) {
                option.setName(getDefaultContainerName(node));
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

    private String getDefaultContainerName(StepNode node) {
        NodePath path = node.getPath();
        String stepStr = path.getNodePathWithoutSpace().replace(NodePath.PathSeparator, "-");
        return StringHelper.escapeNumber(String.format("%s-%s", stepStr, StringHelper.randomString(5)));
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

    private Integer linkRetry(StepNode current, Integer defaultRetry) {
        if (current.hasRetry()) {
            return current.getRetry();
        }

        if (current.hasParent()) {
            Node parent = current.getParent();
            if (parent instanceof StepNode) {
                return linkRetry((StepNode) parent, defaultRetry);
            }
        }

        return defaultRetry;
    }

    private Cache linkCache(Node current) {
        if (current.hasCache()) {
            return current.getCache();
        }

        if (current.hasParent()) {
            Node parent = current.getParent();
            return linkCache(parent);
        }

        return null;
    }

    private Integer linkTimeout(StepNode current, Integer defaultTimeout) {
        if (current.hasTimeout()) {
            return current.getTimeout();
        }

        if (current.hasParent()) {
            Node parent = current.getParent();
            if (parent instanceof StepNode) {
                return linkTimeout((StepNode) parent, defaultTimeout);
            }
        }

        return defaultTimeout;
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

    private List<String> linkScript(StepNode current, ShellIn.ShellType shellType) {
        List<String> output = new LinkedList<>();

        if (current.hasParent()) {
            Node parent = current.getParent();
            if (parent instanceof StepNode) {
                output.addAll(linkScript((StepNode) parent, shellType));
            }
        }

        if (shellType == ShellIn.ShellType.Bash) {
            output.add(current.getBash());
        }

        if (shellType == ShellIn.ShellType.PowerShell) {
            output.add(current.getPwsh());
        }

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
        cmd.addScript(plugin.getBash(), ShellIn.ShellType.Bash);
        cmd.addScript(plugin.getPwsh(), ShellIn.ShellType.PowerShell);

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
