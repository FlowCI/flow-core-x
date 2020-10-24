package com.flowci.core.common.manager;

import com.flowci.domain.Vars;
import groovy.util.ScriptException;

import javax.annotation.Nullable;

public interface ConditionManager {

    boolean run(@Nullable String groovyScript, @Nullable Vars<String> envs) throws ScriptException;
}
