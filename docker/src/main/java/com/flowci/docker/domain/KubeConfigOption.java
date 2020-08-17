package com.flowci.docker.domain;

import lombok.Getter;

import java.io.Reader;

@Getter
public final class KubeConfigOption extends K8sOption {

    private final Reader kubeConfig;

    public KubeConfigOption(String namespace, Reader kubeConfig) {
        super(namespace);
        this.kubeConfig = kubeConfig;
    }
}
