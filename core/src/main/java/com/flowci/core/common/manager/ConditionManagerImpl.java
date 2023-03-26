package com.flowci.core.common.manager;

import com.flowci.domain.Vars;
import com.google.common.base.Strings;
import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.util.ScriptException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class ConditionManagerImpl implements ConditionManager {

    private final int DefaultTimeout = 2; // seconds

    @Autowired
    private ThreadPoolTaskExecutor jobConditionExecutor;

    @Override
    public void verify(@Nullable String condition) throws ScriptException {
        try {
            new GroovyShell().parse(condition);
        } catch (Exception e) {
            throw new ScriptException("Invalid groovy condition: " + e.getMessage());
        }
    }

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
