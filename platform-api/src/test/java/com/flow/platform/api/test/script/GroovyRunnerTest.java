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

package com.flow.platform.api.test.script;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.script.GroovyRunner;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.core.util.ThreadUtil;
import com.google.common.io.Files;
import groovy.util.ScriptException;
import java.io.File;
import java.net.URL;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
public class GroovyRunnerTest {

    private ThreadPoolTaskExecutor executor;

    @Before
    public void initThreadPool() {
        executor = ThreadUtil.createTaskExecutor(1, 1, 1, "ut-groovy");
        executor.initialize();
    }

    @Test
    public void should_eval_script_with_expected_result() throws Throwable {
        // given: env variables
        ClassLoader classLoader = TestBase.class.getClassLoader();
        URL resource = classLoader.getResource("groovy/runnable.groovy");
        File path = new File(resource.getFile());
        String script = Files.toString(path, AppConfig.DEFAULT_CHARSET);

        // when:
        Integer result = GroovyRunner.<Integer>create()
            .putVariable("x", 2)
            .setScript(script)
            .run();

        // then:
        Assert.assertEquals(6, result.intValue());
    }

    @Test(expected = ClassCastException.class)
    public void should_raise_exception_when_incorrect_return_type() throws Throwable {
        String run = GroovyRunner.<String>create().setScript("3 * 5").run();
    }

    @Test(expected = ScriptException.class)
    public void should_raise_exception_when_expect_return_boolean() throws Throwable {
        GroovyRunner.create().setScript("3 * 5").runAndReturnBoolean();
    }

    @Test
    public void should_success_exec_script_within_timeout() throws Throwable {
        Boolean result = GroovyRunner.<Boolean>create()
            .setExecutor(executor)
            .setTimeOut(1)
            .setScript("sleep(100) \n true")
            .run();

        Assert.assertTrue(result);
    }

    @Test(expected = ScriptException.class)
    public void should_raise_exception_when_script_timeout() throws Throwable {
        GroovyRunner.create()
            .setExecutor(executor)
            .setTimeOut(1)
            .setScript("sleep(2000) \n true")
            .run();
    }
}
