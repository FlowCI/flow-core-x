package com.flowci.core.job.util;

import com.flowci.exception.CIException;
import com.flowci.exception.StatusException;

public abstract class Errors {

    public final static CIException AgentOffline = new StatusException("Agent unexpected offline");
}
