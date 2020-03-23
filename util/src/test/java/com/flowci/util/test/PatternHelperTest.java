/*
 * Copyright 2019 flow.ci
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

package com.flowci.util.test;

import com.flowci.util.PatternHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class PatternHelperTest {

    @Test
    public void should_match_web_url() {
        Assert.assertTrue(PatternHelper.isWebURL("http://flow.ci"));
        Assert.assertTrue(PatternHelper.isWebURL("https://192.168.0.1:8080/"));
        Assert.assertFalse(PatternHelper.isWebURL("ttp://flow.ci"));
    }

    @Test
    public void should_match_git_url() {
        Assert.assertTrue(PatternHelper.isGitURL("user@host.com:path/to/repo.git"));
        Assert.assertTrue(PatternHelper.isGitURL("ssh://user@server/project.git"));
        Assert.assertTrue(PatternHelper.isGitURL("git@server:project.git"));
        Assert.assertFalse(PatternHelper.isGitURL("git@server:project"));
    }

    @Test
    public void should_match_email() {
        Assert.assertTrue(PatternHelper.isEmail("user@host.com"));
        Assert.assertFalse(PatternHelper.isEmail("user@hostcom"));
    }

}
