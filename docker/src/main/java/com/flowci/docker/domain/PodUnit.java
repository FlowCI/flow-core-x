package com.flowci.docker.domain;

import io.kubernetes.client.openapi.models.V1Pod;

public class PodUnit implements Unit {

    private final V1Pod pod;

    public PodUnit(V1Pod pod) {
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
}
