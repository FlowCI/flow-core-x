package com.flowci.docker.domain;

import lombok.Getter;

@Getter
public final class KubeConfigOption extends K8sOption {

    private final String kubeConfig;

    public KubeConfigOption(String namespace, String kubeConfig) {
        super(namespace);
        this.kubeConfig = kubeConfig;
    }
}
