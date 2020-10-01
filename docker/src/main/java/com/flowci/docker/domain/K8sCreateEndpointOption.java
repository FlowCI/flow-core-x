package com.flowci.docker.domain;

import lombok.Getter;

@Getter
public class K8sCreateEndpointOption {

    private final String name;

    private final String ip;

    private final Integer port;

    private final String protocol = "TCP";

    public K8sCreateEndpointOption(String name, String ip, Integer port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
    }
}
