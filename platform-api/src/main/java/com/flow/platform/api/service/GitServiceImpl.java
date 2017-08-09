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

import com.flow.platform.api.dao.FlowDao;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.exception.NotFoundException;
import com.flow.platform.exception.IllegalStatusException;
import com.flow.platform.exception.NotImplementedException;
import com.flow.platform.exception.UnsupportedException;
import com.flow.platform.util.git.GitClient;
import com.flow.platform.util.git.GitSshClient;
import com.flow.platform.util.git.model.GitSource;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
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
    private Path workspace;

    @Autowired
    private FlowDao flowDao;

    @Override
    public String fetch(Flow flow, String filePath) {
        GitClient client = gitClientInstance(flow);
        File gitFolder = client.clone(null, Sets.newHashSet(filePath));

        try {
            File targetFile = Paths.get(gitFolder.getParent(), filePath).toFile();
            if (targetFile.exists()) {
                return Files.toString(targetFile, Charset.forName("UTF-8"));
            }

            throw new NotFoundException("Target fetched file doesn't exist");
        } catch (IOException e) {
            throw new IllegalStatusException("Fail to read target fetched file");
        }
    }

    @Override
    public void fetch(Flow flow, String filePath, Consumer<String> callBack) {
        throw new NotImplementedException();
    }

    private GitClient gitClientInstance(Flow flow) {
        GitSource gitSource = GitSource.valueOf(flow.getEnvs().get(FLOW_GIT_SOURCE));
        String gitUrl = flow.getEnvs().get(FLOW_GIT_URL);

        GitClient client = null;
        if (gitSource == GitSource.UNDEFINED) {
            client = new GitSshClient(gitUrl, flowDao.workspace(workspace, flow));
        }

        if (client == null) {
            throw new UnsupportedException(String.format("Git source %s not supported yet", gitSource));
        }
        return client;
    }
}
