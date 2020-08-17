package com.flowci.docker.domain;

import io.fabric8.kubernetes.api.model.Pod;

public class PodUnit implements Unit {

    private final Pod pod;

    public PodUnit(Pod pod) {
        this.pod = pod;
    }

    @Override
    public String getId() {
        return pod.getMetadata().getName();
    }

    @Override
    public String getName() {
        return pod.getMetadata().getName();
    }

    @Override
    public String getStatus() {
        return pod.getStatus().getPhase();
    }
}
