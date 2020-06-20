package com.flowci.core.job.service;

import com.flowci.core.agent.domain.TtyCmd;

/**
 * @author yang
 */
public interface TtyService {

    void execute(TtyCmd.In in);
}
