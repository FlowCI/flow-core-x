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

package com.flow.platform.util.test;

import com.flow.platform.util.SystemUtil;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class SystemUtilTest {

    @Test
    public void should_parse_linux_env() throws Throwable {
        String home = SystemUtil.parseEnv("${HOME}");
        Assert.assertEquals(System.getenv("HOME"), home);

        home = SystemUtil.parseEnv("$HOME");
        Assert.assertEquals(System.getenv("HOME"), home);

        home = SystemUtil.parseEnv("${HOME");
        Assert.assertNull(home);
    }

    @Test
    public void should_replace_path_with_env() throws Throwable {
        // single placeholder
        Path path = SystemUtil.replacePathWithEnv("${HOME}/test");
        Path expected = Paths.get(System.getenv("HOME"), "test");
        Assert.assertEquals(expected, path);

        // two placeholder
        path = SystemUtil.replacePathWithEnv("${HOME}/${HOME}");
        expected = Paths.get(System.getenv("HOME"), System.getenv("HOME"));
        Assert.assertEquals(expected, path);

        // no placeholder
        path = SystemUtil.replacePathWithEnv("/etc/flow.ci");
        expected = Paths.get("/etc/flow.ci");
        Assert.assertEquals(expected, path);
    }
}
