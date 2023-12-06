package com.flowci.core.common.manager;

import com.flowci.common.domain.Vars;
import groovy.util.ScriptException;

import javax.annotation.Nullable;

public interface ConditionManager {

    /**
     * Verify the input condition is groovy script with boolean return or not
     */
    void verify(@Nullable String condition) throws ScriptException;

    boolean run(@Nullable String groovyScript, @Nullable Vars<String> envs) throws ScriptException;
}
