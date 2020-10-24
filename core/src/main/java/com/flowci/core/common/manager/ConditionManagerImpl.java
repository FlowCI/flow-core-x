package com.flowci.core.common.manager;

import com.flowci.domain.Vars;
import com.google.common.base.Strings;
import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.util.ScriptException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Log4j2
@Component
public class ConditionManagerImpl implements ConditionManager {

    private final int DefaultTimeout = 2; // seconds

    @Autowired
    private ThreadPoolTaskExecutor jobConditionExecutor;

    @Override
    public boolean run(String groovyScript, Vars<String> envs) throws ScriptException {
        if (Strings.isNullOrEmpty(groovyScript)) {
            return true;
        }

        Future<Boolean> submit = jobConditionExecutor.submit(() -> {
            Binding binding = new Binding();
            if (envs != null) {
                envs.forEach(binding::setVariable);
            }

            try {
                GroovyShell groovy = new GroovyShell(binding);
                Object value = groovy.evaluate(groovyScript);
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
