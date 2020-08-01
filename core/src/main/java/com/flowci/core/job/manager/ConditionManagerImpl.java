package com.flowci.core.job.manager;

import com.flowci.core.agent.domain.CmdIn;
import com.flowci.core.agent.domain.ShellIn;
import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.util.ScriptException;
import lombok.extern.log4j.Log4j2;
import org.codehaus.groovy.control.CompilationFailedException;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class ConditionManagerImpl implements ConditionManager {

    @Override
    public boolean run(CmdIn in) throws ScriptException {
        if (!(in instanceof ShellIn)) {
            return true;
        }

        ShellIn shell = (ShellIn) in;
        if (!shell.hasCondition()) {
            return true;
        }

        Binding binding = new Binding();
        shell.getInputs().forEach(binding::setVariable);

        try {
            GroovyShell groovy = new GroovyShell(binding);
            Object value = groovy.evaluate(shell.getCondition());
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            throw new ScriptException("The return type is not boolean");
        } catch (GroovyRuntimeException e) {
            throw new ScriptException(e.getMessage());
        }
    }
}
