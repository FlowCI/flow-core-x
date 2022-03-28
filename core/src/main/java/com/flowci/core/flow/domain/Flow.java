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
import com.flowci.core.common.domain.Mongoable;
import com.flowci.core.common.domain.Variables;
import com.flowci.domain.StringVars;
import com.flowci.domain.TypedVars;
import com.flowci.domain.VarValue;
import com.flowci.domain.Vars;
import com.flowci.exception.ArgumentException;
import com.flowci.store.Pathable;
import com.flowci.tree.NodePath;
import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

/**
 * @author yang
 */
@Document(collection = "flow")
@Getter
@Setter
@NoArgsConstructor
@ToString(of = {"name"}, callSuper = true)
public final class Flow extends Mongoable implements Pathable {

    public static Pathable path(String id) {
        Flow flow = new Flow();
        flow.setId(id);
        return flow;
    }

    public static void validateName(String name) {
        if (!NodePath.validate(name)) {
            String message = "Illegal flow name {0}, the length cannot over 100 and '*' ',' is not available";
            throw new ArgumentException(message, name);
        }
    }

    public enum Status {
        PENDING,

        CONFIRMED
    }

    @Indexed(name = "index_flow_name")
    private String name;

    private Status status = Status.PENDING;

    private boolean isYamlFromRepo;

    private String yamlRepoBranch = "master";

    private int jobTimeout = 1800; // timeout while job queuing

    private int stepTimeout = 900; // job step timeout in second;

    private String cron;

    // variables from yml
    private Vars<String> variables = new StringVars();

    // variables for flow obj only
    private Vars<VarValue> locally = new TypedVars();

    private WebhookStatus webhookStatus;

    public Flow(String name) {
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
        VarValue cnVal = locally.get(name);
        if (!Objects.isNull(cnVal)) {
            return cnVal.getData();
        }

        String cn = variables.get(name);
        if (StringHelper.hasValue(cn)) {
            return cn;
        }

        return StringHelper.EMPTY;
    }
}
