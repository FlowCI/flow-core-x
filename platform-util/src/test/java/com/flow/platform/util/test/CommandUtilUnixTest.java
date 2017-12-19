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

import com.flow.platform.util.CommandUtil;
import com.flow.platform.util.SystemUtil;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author yang
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemUtil.class, CommandUtil.class})
public class CommandUtilUnixTest {

    @Before
    public void init () {
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getProperty("os.name")).thenReturn("Mac OS X");
        PowerMockito.when(System.getProperty("user.home")).thenReturn("/user/home");
        PowerMockito.when(System.getenv("HOME")).thenReturn("/user/home");
    }

    @Test
    public void should_get_correct_script() {
        // should get shell executor
        Assert.assertEquals("/bin/bash", CommandUtil.shellExecutor());

        // should get home path
        Assert.assertEquals("${HOME}", CommandUtil.home());

        // should get correct set variable script
        Assert.assertEquals("export VAR=1", CommandUtil.setVariable("VAR", "1"));

        // should get correct get variable script
        Assert.assertEquals("${VAR}", CommandUtil.getVariable("VAR"));
    }

    @Test
    public void should_parse_variable_to_value() {
        // parse home with bracket
        String home = CommandUtil.parseVariable("${HOME}");
        Assert.assertNotNull(home);
        Assert.assertEquals(System.getenv("HOME"), home);

        // parse home without bracket
        home = CommandUtil.parseVariable("$HOME");
        Assert.assertNotNull(home);
        Assert.assertEquals(System.getenv("HOME"), home);

        // parse java property
        home = CommandUtil.parseVariable("@{user.home}");
        Assert.assertNotNull(home);
        Assert.assertEquals("/user/home", home);
    }

    @Test
    public void should_replace_path_with_env() throws Throwable {
        // single placeholder
        Path path = CommandUtil.absolutePath(Paths.get("${HOME}", "test").toString());
        Assert.assertEquals("/user/home/test", path.toString());

        // two placeholder
        path = CommandUtil.absolutePath(Paths.get("${HOME}", "${HOME}").toString());
        Assert.assertEquals("/user/home/user/home", path.toString());

        // no placeholder
        path = CommandUtil.absolutePath("/etc/flow.ci");
        Assert.assertEquals("/etc/flow.ci", path.toString());
    }
}
