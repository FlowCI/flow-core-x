package com.flowci.core.agent.domain;

/**
 * Cmd output from callback queue,
 * first byte is indicator to indicate cmd out type
 *
 * @author yang
 */
public interface CmdOut {

    /**
     * ShellOut
     */
    byte ShellOutInd = 1;

    /**
     * TtyOut
     */
    byte TtyOutInd = 2;
}
