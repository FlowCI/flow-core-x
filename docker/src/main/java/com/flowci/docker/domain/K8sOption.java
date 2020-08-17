package com.flowci.docker.domain;

import lombok.Getter;

@Getter
public abstract class K8sOption {

    private final String namespace;

    public K8sOption(String namespace) {
        this.namespace = namespace;
    }
}
