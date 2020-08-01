package com.flowci.core.job.manager;

import com.flowci.core.agent.domain.CmdIn;
import com.flowci.core.agent.domain.ShellIn;
import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.util.ScriptException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Log4j2
@Component
public class ConditionManagerImpl implements ConditionManager {

    private final int DefaultTimeout = 2; // seconds

    @Autowired
    private ThreadPoolTaskExecutor jobConditionExecutor;

    @Override
    public boolean run(CmdIn in) throws ScriptException {
        if (!(in instanceof ShellIn)) {
            return true;
        }

        ShellIn shell = (ShellIn) in;
        if (!shell.hasCondition()) {
            return true;
        }

        Future<Boolean> submit = jobConditionExecutor.submit(() -> {
            Binding binding = new Binding();
            shell.getInputs().forEach(binding::setVariable);

            try {
                GroovyShell groovy = new GroovyShell(binding);
                Object value = groovy.evaluate(shell.getCondition());
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }
                throw new Exception("The return type is not boolean");
            } catch (GroovyRuntimeException e) {
                throw new Exception(e.getMessage());
            }
        });

        try {
            return submit.get(DefaultTimeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new ScriptException("Condition script interrupted");
        } catch (ExecutionException e) {
            throw new ScriptException(e.getMessage());
        } catch (TimeoutException e) {
            submit.cancel(true);
            throw new ScriptException("Condition script timeout");
        }
    }
}
