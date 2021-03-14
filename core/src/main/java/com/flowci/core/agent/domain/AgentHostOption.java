/*
 * Copyright 2020 flow.ci
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

package com.flowci.core.agent.domain;

import com.flowci.exception.ArgumentException;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class AgentHostOption {

    private String id;

    private Set<String> tags = new HashSet<>();

    private int exitOnIdle;

    @NotNull
    private AgentHost.Type type;

    @NotEmpty
    private String name;

    private String secret;

    private String user;

    private String ip;

    private String namespace;

    @Min(1)
    @Max(Integer.MAX_VALUE)
    private int port = 22;

    @Min(1)
    @Max(Integer.MAX_VALUE)
    private int maxSize = 5;

    public AgentHost toObj() {
        if (type == AgentHost.Type.SSH) {
            SshAgentHost host = new SshAgentHost();
            host.setId(id);
            host.setName(name);
            host.setTags(tags);
            host.setSecret(secret);
            host.setUser(user);
            host.setIp(ip);
            host.setMaxSize(maxSize);
            host.setPort(port);
            host.setExitOnIdle(exitOnIdle);
            return host;
        }

        if (type == AgentHost.Type.LocalUnixSocket) {
            LocalUnixAgentHost host = new LocalUnixAgentHost();
            host.setId(id);
            host.setName(name);
            host.setTags(tags);
            host.setMaxSize(maxSize);
            host.setExitOnIdle(exitOnIdle);
            return host;
        }

        if (type == AgentHost.Type.K8s) {
            K8sAgentHost host = new K8sAgentHost();
            host.setId(id);
            host.setName(name);
            host.setTags(tags);
            host.setSecret(secret);
            host.setNamespace(namespace);
            host.setMaxSize(maxSize);
            host.setExitOnIdle(exitOnIdle);
            return host;
        }

        throw new ArgumentException("Unsupported host type");
    }
}
