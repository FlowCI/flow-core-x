/*
 * Copyright 2017 flow.ci
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

package com.flow.platform.api.envs.handler;

import static com.flow.platform.api.envs.GitEnvs.FLOW_GIT_CREDENTIAL;

import com.flow.platform.api.domain.node.Node;
import com.flow.platform.api.envs.EnvKey;
import com.flow.platform.api.envs.EnvUtil;
import com.flow.platform.api.service.CredentialService;
import com.flow.platform.api.service.node.NodeService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yh@firim
 */

@Component
public class FlowCredentialEnvHandler extends EnvHandler {

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private NodeService nodeService;

    @Override
    public EnvKey env() {
        return FLOW_GIT_CREDENTIAL;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    void onHandle(Node node, String value) {
        Map<String, String> credentialEnvs = credentialService.findByName(value);
        EnvUtil.keepNewlineForEnv(credentialEnvs, null);
        node.putAll(credentialEnvs);
    }

    @Override
    void onUnHandle(Node node, String value) {

    }
}
