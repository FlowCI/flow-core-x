package com.flowci.core.job.service;

/**
 * @author yang
 */
public interface TtyService {

    /**
     * Send 'Open' to current agent to open tty session
     */
    void open(String jobId);

    /**
     * Send shell script to agent
     */
    void shell(String jobId, String script);

    /**
     * Close current tty session
     */
    void close(String jobId);
}
