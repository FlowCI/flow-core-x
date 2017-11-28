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

package com.flow.platform.plugin.test.util;

import com.flow.platform.plugin.util.GitHelperUtil;
import com.flow.platform.util.git.JGitUtil;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.eclipse.jgit.api.Git;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author yh@fir.im
 */


public class GitHelperUtilTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void should_get_latest_tag() throws Throwable {
        // init local repo
        File mocGit = folder.newFolder("test.git");
        File gitCloneMocGit = folder.newFolder("test");
        JGitUtil.init(mocGit.toPath(), true);
        JGitUtil.clone(mocGit.toString(), gitCloneMocGit.toPath());

        // git commit something
        Files.createFile(Paths.get(gitCloneMocGit.toString(), "readme.md"));
        Git git = Git.open(gitCloneMocGit);
        git.add().addFilepattern(".").call();
        git.commit().setMessage("test").call();
        JGitUtil.push(gitCloneMocGit.toPath(), "origin", "master");

        // git push tag
        git.tag().setName("1.0").setMessage("add tag 1.0").call();
        JGitUtil.push(gitCloneMocGit.toPath(), "origin", "1.0");

        String gitUrl = mocGit.toString();

        File gitFile = folder.newFolder("info");

        // when clone code
        JGitUtil.clone(gitUrl, gitFile.toPath());

        // then should get latest tag
        Assert.assertEquals("1.0", GitHelperUtil.getLatestTag(gitFile.toPath()));

    }

}
