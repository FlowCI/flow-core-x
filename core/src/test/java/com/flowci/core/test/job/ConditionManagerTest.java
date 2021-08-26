package com.flowci.core.test.job;

import com.flowci.core.common.manager.ConditionManager;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.StringVars;
import groovy.util.ScriptException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ConditionManagerTest extends SpringScenario {

    @Autowired
    private ConditionManager conditionManager;

    @Test(expected = ScriptException.class)
    public void should_verify_condition() throws ScriptException {
        String expression = "if(a==1 return 1;";
        conditionManager.verify(expression);
    }

    @Test
    public void should_run_groovy_condition() throws ScriptException {
        StringVars vars = new StringVars();
        vars.put("foo", "helloword");

        String groovy = "println \"$foo\"; "
                + "return true;";

        Assert.assertTrue(conditionManager.run(groovy, vars));
    }

    @Test(expected = ScriptException.class)
    public void should_throw_exception_if_wrong_return_type() throws ScriptException {
        StringVars vars = new StringVars();
        vars.put("foo", "helloword");

        String groovy = "println \"$foo\"; "
                + "hello = \"1234\";"
                + "return hello;";

        conditionManager.run(groovy, vars);
    }

    @Test(expected = ScriptException.class)
    public void should_throw_exception_if_timeout() throws ScriptException {
        StringVars vars = new StringVars();
        vars.put("foo", "helloword");

        String groovy = (
                "sleep(6000); "
                        + "println \"$foo\"; "
                        + "hello = \"1234\";"
                        + "return true;"
        );

        conditionManager.run(groovy, vars);
    }
}
