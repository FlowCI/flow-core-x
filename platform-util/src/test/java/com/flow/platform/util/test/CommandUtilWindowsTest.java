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
public class CommandUtilWindowsTest {

    @Before
    public void init () {
        PowerMockito.mockStatic(System.class);
        PowerMockito.when(System.getProperty("os.name")).thenReturn("Windows 10");
        PowerMockito.when(System.getProperty("user.home")).thenReturn("C:\\Users\\home");
        PowerMockito.when(System.getenv("HOMEPATH")).thenReturn("C:\\Users\\home");
    }

    @Test
    public void should_get_correct_script() {

        // should get shell executor
        Assert.assertEquals("cmd.exe", CommandUtil.shellExecutor());

        // should get home path
        Assert.assertEquals("%HOMEPATH%", CommandUtil.home());

        // should get correct set variable script
        Assert.assertEquals("set VAR=1", CommandUtil.setVariable("VAR", "1"));

        // should get correct get variable script
        Assert.assertEquals("%VAR%", CommandUtil.getVariable("VAR"));
    }

    @Test
    public void should_parse_variable_to_value() {
        // parse windows env
        String home = CommandUtil.parseVariable("%HOMEPATH%");
        Assert.assertNotNull(home);
        Assert.assertEquals(System.getenv("HOMEPATH"), home);
        Assert.assertEquals("C:\\Users\\home", home);

        // parse java property
        home = CommandUtil.parseVariable("@{user.home}");
        Assert.assertNotNull(home);
        Assert.assertEquals("C:\\Users\\home", home);
    }

    @Test
    public void should_replace_path_with_env() throws Throwable {
        PowerMockito.when(System.getenv("HELLO")).thenReturn("myfolder");

        // single placeholder
        Path path = CommandUtil.absolutePath("%HOMEPATH%\\test");
        Assert.assertEquals("C:\\Users\\home\\test", path.toString());

        // two placeholder
        path = CommandUtil.absolutePath("%HOMEPATH%\\%HELLO%");
        Assert.assertEquals("C:\\Users\\home\\myfolder", path.toString());

        // no placeholder
        path = CommandUtil.absolutePath("C:\\System\\Program Files");
        Assert.assertEquals("C:\\System\\Program Files", path.toString());

        // flow.ci path
        path = CommandUtil.absolutePath("@{user.home}|Program Files");
        Assert.assertEquals("C:\\Users\\home\\Program Files", path.toString());
    }
}
