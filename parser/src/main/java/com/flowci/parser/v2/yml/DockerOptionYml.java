package com.flowci.parser.v2.yml;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class DockerOptionYml {

    private String image;

    private String auth; // auth secret for private docker registry

    private String name;

    private String network;

    private List<String> ports;

    private List<String> entrypoint;

    private List<String> command;

    private Map<String, String> environment;

    private Boolean is_runtime;

    private Boolean stop_on_finish;

    private Boolean delete_on_finish;
}
