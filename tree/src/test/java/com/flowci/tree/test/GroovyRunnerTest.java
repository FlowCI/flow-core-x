/*
 * Copyright 2018 flow.ci
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

package com.flowci.tree.test;

import com.flowci.domain.StringVars;
import com.flowci.tree.GroovyRunner;
import groovy.util.ScriptException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class GroovyRunnerTest {

    @Test
    public void should_run_script_with_variables() throws ScriptException {
        StringVars variables = new StringVars();
        variables.put("FLOWCI_TEST", "123");

        String script = "println \"$FLOWCI_TEST\"; "
            + "return true;";
        GroovyRunner<Boolean> runner = GroovyRunner.create(2, script, variables);

        Assert.assertTrue(runner.run());
    }

    @Test(expected = ScriptException.class)
    public void should_run_script_with_timeout() throws ScriptException {
        String script = "println \"$FLOWCI_TEST\"; "
            + "sleep(3000);"
            + "return true;";

        GroovyRunner.create(2, script).run();
    }
}
