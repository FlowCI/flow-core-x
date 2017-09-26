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

package com.flow.platform.api.git;

import com.flow.platform.api.domain.envs.GitEnvs;
import com.flow.platform.api.domain.node.Node;
import com.flow.platform.util.git.GitClient;
import com.flow.platform.util.git.GitHttpClient;
import java.nio.file.Path;

/**
 * @author yang
 */
public class GitHttpClientBuilder extends GitClientBuilder {

    private final String user;

    private final String pass;

    public GitHttpClientBuilder(Node node, Path sourceFolder) {
        super(node, sourceFolder);

        user = node.getEnv(GitEnvs.FLOW_GIT_HTTP_USER);
        pass = node.getEnv(GitEnvs.FLOW_GIT_HTTP_PASS);
    }

    @Override
    public GitClient build() {
        return new GitHttpClient(url, sourceFolder, user, pass);
    }
}
