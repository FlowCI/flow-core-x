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

package com.flow.platform.plugin.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.flow.platform.plugin.domain.Plugin;
import com.flow.platform.plugin.service.PluginService;
import com.flow.platform.plugin.service.PluginStoreService;
import com.flow.platform.util.git.JGitUtil;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.Charsets;
import org.eclipse.jgit.api.Git;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author gyfirim
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {AppConfig.class})
@FixMethodOrder(MethodSorters.JVM)
public abstract class TestBase {

    protected final static String DEMO_GIT_NAME = "firCi";

    protected final static String GIT_SUFFIX = ".git";

    protected File mocGit;

    protected File gitCloneMocGit;

    @Rule
    public WireMockRule wiremock = new WireMockRule(8080);


    {
        try {
            stubDemo();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Autowired
    protected PluginService pluginService;

    @Autowired
    protected PluginStoreService pluginStoreService;

    // git clone folder
    @Autowired
    protected Path gitWorkspace;

    // local library
    @Autowired
    protected Path gitCacheWorkspace;

    @Autowired
    protected ApplicationContext applicationContext;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected void stubDemo() throws IOException {
        wiremock.resetAll();
        wiremock.stubFor(
            get(urlEqualTo("/repos/plugin.json"))
                .willReturn(aResponse().withStatus(200).withBody(getResource("repos_demo.json"))));
    }

    private String getResource(String fileName) throws IOException {
        Path path = Paths.get(TestBase.class.getClassLoader().getResource(fileName).getPath());
        return Files.toString(path.toFile(), Charsets.UTF_8);
    }


    protected void initGit() {

        try {
            mocGit = temporaryFolder.newFolder(DEMO_GIT_NAME + GIT_SUFFIX);
            gitCloneMocGit = temporaryFolder.newFolder(DEMO_GIT_NAME);
            initGit();

            JGitUtil.initBare(mocGit.toPath(), true);
            JGitUtil.clone(mocGit.toString(), gitCloneMocGit.toPath());

            // git commit something
            java.nio.file.Files.createFile(Paths.get(gitCloneMocGit.toString(), "readme.md"));
            Git git = Git.open(gitCloneMocGit);
            git.add().addFilepattern(".").call();
            git.commit().setMessage("firCi").call();
            JGitUtil.push(gitCloneMocGit.toPath(), "origin", "master");

            // git push tag
            git.tag().setName("1.0").setMessage("add tag 1.0").call();
            JGitUtil.push(gitCloneMocGit.toPath(), "origin", "1.0");
        } catch (Throwable throwable) {

        }

        // update plugin to local git
        for (Plugin plugin : pluginStoreService.list()) {
            plugin.setDetails(mocGit.getParent() + "/firCi");
            pluginStoreService.update(plugin);
        }
    }
}
