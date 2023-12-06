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

package com.flowci.common.helper;

import com.flowci.common.helper.PatternHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author yang
 */
public class PatternHelperTest {

    @Test
    public void should_match_web_url() {
        assertTrue(PatternHelper.isWebURL("http://flow.ci"));
        assertTrue(PatternHelper.isWebURL("https://192.168.0.1:8080/"));
        assertFalse(PatternHelper.isWebURL("ttp://flow.ci"));
    }

    @Test
    public void should_match_git_url() {
        assertTrue(PatternHelper.isGitURL("user@host.com:path/to/repo.git"));
        assertTrue(PatternHelper.isGitURL("ssh://user@server/project.git"));
        assertTrue(PatternHelper.isGitURL("git@server:project.git"));
        assertFalse(PatternHelper.isGitURL("git@server:project"));
    }

    @Test
    public void should_match_email() {
        assertTrue(PatternHelper.isEmail("user@host.com"));
        assertFalse(PatternHelper.isEmail("user@hostcom"));
    }
}
