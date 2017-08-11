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

package com.flow.platform.api.test.service;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.domain.Flow;
import com.flow.platform.api.service.GitService;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.util.git.model.GitSource;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileSystemUtils;

/**
 * @author yang
 */
public class GitServiceTest extends TestBase {

    @Autowired
    private GitService gitService;

    @Autowired
    private Path workspace;

    @Test
//    @Ignore("since ssh public key should added to git repo")
    public void should_fetch_git_file() throws Throwable {
        Flow dummyFlow = new Flow("/flow-test", "flow-test");
        dummyFlow.getEnvs().put("FLOW_GIT_SOURCE", GitSource.UNDEFINED_SSH.name());
        dummyFlow.getEnvs().put("FLOW_GIT_URL", "git@github.com:flow-ci-plugin/for-testing.git");

        String content = gitService.fetch(dummyFlow, AppConfig.DEFAULT_YML_FILE);
        Assert.assertNotNull(content);
    }

    @After
    public void after() throws Throwable {
        FileSystemUtils.deleteRecursively(workspace.toFile());
    }
}
