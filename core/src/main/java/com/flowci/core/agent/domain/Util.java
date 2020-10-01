package com.flowci.core.agent.domain;

public abstract class Util {

    private static final String LockPathSuffix = "-lock";

    public static String getZkLockPath(String root, Agent agent) {
        return root + Agent.PATH_SLASH + agent.getId() + LockPathSuffix;
    }

    public static String getAgentIdFromLockPath(String lockPath) {
        return lockPath.replace(LockPathSuffix, "");
    }
}
