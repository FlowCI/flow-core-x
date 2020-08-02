package com.flowci.core.test.job;

import com.flowci.core.agent.domain.ShellIn;
import com.flowci.core.job.manager.ConditionManager;
import com.flowci.core.test.SpringScenario;
import groovy.util.ScriptException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;

public class ConditionManagerTest extends SpringScenario {

    @Autowired
    private ConditionManager conditionManager;

    @Test
    public void should_run_groovy_condition() throws ScriptException {
        ShellIn in = new ShellIn();
        in.getInputs().put("foo", "helloword");
        in.setCondition(
                "println \"$foo\"; "
                        + "return true;"
        );

        Assert.assertTrue(conditionManager.run(in));
    }

    @Test(expected = ScriptException.class)
    public void should_throw_exception_if_wrong_return_type() throws ScriptException {
        ShellIn in = new ShellIn();
        in.getInputs().put("foo", "helloword");
        in.setCondition(
                "println \"$foo\"; "
                        + "hello = \"1234\";"
                        + "return hello;"
        );

        conditionManager.run(in);
    }

    @Test(expected = ScriptException.class)
    public void should_throw_exception_if_timeout() throws ScriptException {
        ShellIn in = new ShellIn();
        in.getInputs().put("foo", "helloword");
        in.setCondition(
                "sleep(6000); "
                        + "println \"$foo\"; "
                        + "hello = \"1234\";"
                        + "return true;"
        );

        conditionManager.run(in);
    }
}
