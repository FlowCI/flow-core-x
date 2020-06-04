package com.flowci.core.agent.domain;

/**
 * Kill current executing shell command
 */
public final class ShellKill extends CmdIn {

    public ShellKill() {
        super(Type.KILL);
    }
}
