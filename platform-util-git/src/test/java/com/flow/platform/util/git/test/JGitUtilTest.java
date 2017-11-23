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

package com.flow.platform.util.git.test;

import com.flow.platform.util.git.JGitUtil;
import java.io.File;
import org.eclipse.jgit.lib.Repository;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author yang
 */
public class JGitUtilTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void should_init_git_with_bare() throws Throwable {
        File gitDir = folder.newFolder("hello.git");
        JGitUtil.initBare(gitDir.toPath());
        Assert.assertTrue(gitDir.exists());

        Repository repo = JGitUtil.getRepo(gitDir.toPath());
        Assert.assertNotNull(repo);
    }

}
