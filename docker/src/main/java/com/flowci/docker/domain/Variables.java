package com.flowci.docker.domain;

import java.util.HashMap;
import java.util.Map;

public abstract class Variables {

    public static final String NODE_NAME = "K8S_NODE_NAME";

    public static final String POD_NAME = "K8S_POD_NAME";

    public static final String POD_IP = "K8S_POD_IP";

    public static final String NAMESPACE = "K8S_NAMESPACE";

    public static final Map<String, String> PodVarsAndFieldPath = new HashMap<>();

    static {
        PodVarsAndFieldPath.put(NODE_NAME, "spec.nodeName");
        PodVarsAndFieldPath.put(POD_NAME, "metadata.name");
        PodVarsAndFieldPath.put(NAMESPACE, "metadata.namespace");
        PodVarsAndFieldPath.put(POD_IP, "status.podIP");
    }
}
