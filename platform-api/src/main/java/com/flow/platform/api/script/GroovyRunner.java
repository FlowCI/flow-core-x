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

package com.flow.platform.api.script;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.util.ScriptException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.codehaus.groovy.control.CompilationFailedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
public class GroovyRunner<T> {

    public static <T> GroovyRunner<T> create() {
        return new GroovyRunner<>();
    }

    private Binding binding = new Binding();

    private Script script = null;

    private ThreadPoolTaskExecutor executor = null;

    private int timeout = 10; // in seconds

    private GroovyRunner() {

    }

    public GroovyRunner<T> putVariable(String name, Object value) {
        binding.setVariable(name, value);
        return this;
    }

    public GroovyRunner<T> putVariables(Map<String, Object> vars) {
        if (vars != null) {
            for (Map.Entry<String, Object> entry : vars.entrySet()) {
                binding.setVariable(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    public GroovyRunner<T> setScript(String source) throws ScriptException {
        try {
            GroovyShell shell = new GroovyShell(binding);
            script = shell.parse(source);
            return this;
        } catch (CompilationFailedException e) {
            throw new ScriptException("Groovy script exception: " + e.getMessage());
        }
    }

    public GroovyRunner<T> setExecutor(ThreadPoolTaskExecutor executor) {
        this.executor = executor;
        return this;
    }

    public GroovyRunner<T> setTimeOut(int seconds) {
        this.timeout = seconds;
        return this;
    }

    public T run() throws ScriptException {
        if (script == null) {
            throw new ScriptException("Script not been set");
        }

        if (executor == null) {
            return (T) script.run();
        }

        Future<T> task = executor.submit(() -> (T) script.run());

        try {
            return task.get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new ScriptException("the script been interrupted");
        } catch (ExecutionException e) {
            throw new ScriptException(e.getMessage());
        } catch (TimeoutException e) {
            throw new ScriptException("The script been timeout");
        }
    }

    public Boolean runAndReturnBoolean() throws ScriptException {
        try {
            return (Boolean) run();
        } catch (ClassCastException e) {
            throw new ScriptException(e.getMessage());
        }
    }

}
