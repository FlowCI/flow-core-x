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

package com.flowci.tree.yml;

import com.flowci.tree.Node;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author yang
 */
@Setter
@Getter
@NoArgsConstructor
public class StepNode extends YmlNode {

    private final static String DEFAULT_NAME_PREFIX = "step-";

    private String before;

    private String script;

    private String plugin;

    private List<String> exports = new LinkedList<>();

    private Boolean allow_failure = false;

    private Boolean tail = false;

    StepNode(Node node) {
        setName(node.getName());
        setEnvs(node.getEnvironments());
        setScript(node.getScript());
        setPlugin(node.getPlugin());
        setAllow_failure(node.isAllowFailure() == Node.ALLOW_FAILURE_DEFAULT ? null : node.isAllowFailure());
        setTail(node.isTail() == Node.IS_TAIL_DEFAULT ? null : node.isTail());
    }

    @Override
    public Node toNode(int index) {
        String name = getName();
        Node node = new Node(Strings.isNullOrEmpty(name) ? DEFAULT_NAME_PREFIX + index : name);
        node.setBefore(before);
        node.setScript(script);
        node.setPlugin(plugin);
        node.setExports(Sets.newHashSet(exports));
        node.setAllowFailure(allow_failure);
        node.setTail(tail);
        node.setEnvironments(getVariableMap());
        return node;
    }
}
