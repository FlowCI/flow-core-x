package com.flowci.core.job.manager;

import com.flowci.core.agent.domain.CmdIn;
import groovy.util.ScriptException;

public interface ConditionManager {

    boolean run(CmdIn in) throws ScriptException;
}
