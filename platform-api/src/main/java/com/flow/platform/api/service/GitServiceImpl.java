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

package com.flow.platform.api.service;

import com.flow.platform.api.domain.Flow;
import com.flow.platform.exception.UnsupportedException;
import com.flow.platform.util.git.GitClient;
import com.flow.platform.util.git.GitSshClient;
import com.flow.platform.util.git.model.GitSource;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service
public class GitServiceImpl implements GitService {

    private final static String FLOW_GIT_SOURCE = "FLOW_GIT_SOURCE";
    private final static String FLOW_GIT_URL = "FLOW_GIT_URL";

    @Autowired
    private Path workingDir;

    @Override
    public String fetch(Flow flow, String filePath) {
        GitSource gitSource = GitSource.valueOf(flow.getEnvs().get(FLOW_GIT_SOURCE));
        String gitUrl = flow.getEnvs().get(FLOW_GIT_URL);

        GitClient client = null;
        if (gitSource == GitSource.UNDEFINED) {
            client = new GitSshClient(gitUrl, workingDir);
        }

        if (client == null) {
            throw new UnsupportedException(String.format("Git source %s not supported yet", gitSource));
        }

        return null;
    }
}
