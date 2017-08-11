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

import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.service.GitService.Env;
import com.flow.platform.util.git.GitClient;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author yang
 */
public abstract class GitClientBuilder {

    // the folder in the flow workspace
    private final static String SOURCE_FOLDER_NAME = "source";

    protected String url;

    protected String branch;

    protected Path flowWorkspace;

    protected Path codeFolder;

    public GitClientBuilder(final Flow flow, final Path flowWorkspace) {
        this.flowWorkspace = flowWorkspace;
        url = flow.getEnvs().get(Env.FLOW_GIT_URL);
        branch = flow.getEnvs().get(Env.FLOW_GIT_BRANCH);
        codeFolder = Paths.get(flowWorkspace.toString(), SOURCE_FOLDER_NAME);
    }

    public abstract GitClient build();
}
