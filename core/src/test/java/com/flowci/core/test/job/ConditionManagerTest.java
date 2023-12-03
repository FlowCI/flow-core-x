package com.flowci.core.test.job;

import com.flowci.core.common.manager.ConditionManager;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.StringVars;
import groovy.util.ScriptException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConditionManagerTest extends SpringScenario {

    @Autowired
    private ConditionManager conditionManager;

    @Test
    void should_verify_condition() {
        Assertions.assertThrows(ScriptException.class, () -> {
            String expression = "if(a==1 return 1;";
            conditionManager.verify(expression);
        });
    }

    @Test
    void should_run_groovy_condition() throws ScriptException {
        StringVars vars = new StringVars();
        vars.put("foo", "helloword");

        String groovy = "println \"$foo\"; "
                + "return true;";

        assertTrue(conditionManager.run(groovy, vars));
    }

    @Test
    void should_throw_exception_if_wrong_return_type() {
        StringVars vars = new StringVars();
        vars.put("foo", "helloword");

        String groovy = "println \"$foo\"; "
                + "hello = \"1234\";"
                + "return hello;";

        assertThrows(ScriptException.class, () -> conditionManager.run(groovy, vars));
    }

    @Test
    void should_throw_exception_if_timeout() throws ScriptException {
        StringVars vars = new StringVars();
        vars.put("foo", "helloword");

        String groovy = (
                "sleep(6000); "
                        + "println \"$foo\"; "
                        + "hello = \"1234\";"
                        + "return true;"
        );

        assertThrows(ScriptException.class, () -> conditionManager.run(groovy, vars));
    }
}
