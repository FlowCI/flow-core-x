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

package com.flowci.tree;

import com.flowci.domain.Vars;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.util.ScriptException;
import org.codehaus.groovy.control.CompilationFailedException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author yang
 */
public class GroovyRunner<T> {

    public static <T> GroovyRunner<T> create(int timeout, String script, Vars<String> vars) throws ScriptException {
        return new GroovyRunner<T>(timeout)
            .setScript(script)
            .putVariables(vars);
    }

    public static <T> GroovyRunner<T> create(int timeout, String script) throws ScriptException {
        return create(timeout, script, null);
    }

    private Script script = null;

    private final Binding binding = new Binding();

    /**
     * Timeout in seconds
     */
    private final int timeout;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private GroovyRunner(int timeout) {
        this.timeout = timeout;
    }

    public T run() throws ScriptException {
        if (Objects.isNull(script)) {
            throw new ScriptException("Script not been set");
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

    private GroovyRunner<T> putVariables(Map<String, String> vars) {
        if (Objects.isNull(vars)) {
            return this;
        }

        for (Map.Entry<String, String> entry : vars.entrySet()) {
            binding.setVariable(entry.getKey(), entry.getValue());
        }

        return this;
    }

    private GroovyRunner<T> setScript(String source) throws ScriptException {
        try {
            GroovyShell shell = new GroovyShell(binding);
            script = shell.parse(source);
            return this;
        } catch (CompilationFailedException e) {
            throw new ScriptException("Script compile failed: " + e.getMessage());
        }
    }

}
