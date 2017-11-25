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
        String gitUrl = "https://github.com/yunheli/info.git";

        File gitFile = folder.newFolder("info");

        // when clone code
        JGitUtil.clone(gitUrl, gitFile.toPath());

        // then should get latest tag
        Assert.assertEquals("2.3.1", GitHelperUtil.getLatestTag(gitFile.toPath()));

    }

}
