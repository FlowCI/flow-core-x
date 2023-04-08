/*
 * Copyright (c) 2018 flow.ci
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

package com.flowci.core.flow.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.flowci.core.common.domain.Variables;
import com.flowci.domain.StringVars;
import com.flowci.domain.VarValue;
import com.flowci.domain.Vars;
import com.flowci.exception.ArgumentException;
import com.flowci.domain.node.NodePath;
import com.flowci.store.Pathable;
import com.flowci.util.StringHelper;
import com.google.common.collect.ImmutableSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;
import java.util.Set;

/**
 * @author yang
 */
@Getter
@Setter
@Document(collection = "flow")
@EqualsAndHashCode(callSuper = true)
public final class Flow extends FlowItem implements Pathable {

    private static final Set<String> reservedFlowNames = ImmutableSet.<String>builder()
            .add("flow")
            .add("flows")
            .add("group")
            .add("groups")
            .add("template")
            .add("templates")
            .build();

    public static Pathable path(String id) {
        Flow flow = new Flow();
        flow.setId(id);
        return flow;
    }

    public static void validateName(String name) {
        if (reservedFlowNames.contains(name.toLowerCase())) {
            throw new ArgumentException("flow name {0} cannot be used, it's reserved by system", name);
        }

        if (!NodePath.validate(name)) {
            String message = "Illegal flow name {0}, the length cannot over 100 and '*' ',' is not available";
            throw new ArgumentException(message, name);
        }
    }

    private boolean isYamlFromRepo;

    private String yamlRepoBranch = "master";

    private int jobTimeout = 1800; // timeout while job queuing

    private int stepTimeout = 900; // job step timeout in second;

    private String cron;

    // variables from yml
    private Vars<String> readOnlyVars = new StringVars();

    private WebhookStatus webhookStatus;

    @JsonInclude()
    @Transient
    private FlowGroup parent;

    public Flow() {
        this.type = Type.Flow;
    }

    public Flow(String name) {
        this();
        this.name = name;
    }

    @JsonIgnore
    public boolean hasCron() {
        return StringHelper.hasValue(cron);
    }

    @JsonIgnore
    public String getQueueName() {
        return "flow.q." + id + ".job";
    }

    @JsonIgnore
    @Override
    public String pathName() {
        return getId();
    }

    public String getCredentialName() {
        return findVar(Variables.Git.SECRET);
    }

    public String getGitUrl() {
        return findVar(Variables.Git.URL);
    }

    /**
     * Get credential name from vars, local var has top priority
     */
    private String findVar(String name) {
        VarValue cnVal = vars.get(name);
        if (!Objects.isNull(cnVal)) {
            return cnVal.getData();
        }

        String cn = readOnlyVars.get(name);
        if (StringHelper.hasValue(cn)) {
            return cn;
        }

        return StringHelper.EMPTY;
    }
}

