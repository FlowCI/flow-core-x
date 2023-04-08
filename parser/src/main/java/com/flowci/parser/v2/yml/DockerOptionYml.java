package com.flowci.parser.v2.yml;

import com.flowci.domain.node.DockerOption;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Setter
@Getter
public class DockerOptionYml implements Convertable<DockerOption, Void> {

    private String image;

    private String auth; // auth secret for private docker registry

    private String name;

    private String network;

    private List<String> ports;

    private List<String> entrypoint;

    private List<String> command;

    private Map<String, String> environment;

    private Boolean is_runtime = Boolean.FALSE;

    private Boolean stop_on_finish = Boolean.TRUE;

    private Boolean delete_on_finish = Boolean.TRUE;

    @Override
    public DockerOption convert(Void ...ignore) {
        Objects.requireNonNull(image, "Docker image must be specified");

        return DockerOption.builder()
                .image(image)
                .auth(auth)
                .name(name)
                .network(network)
                .ports(ports)
                .entrypoint(entrypoint)
                .command(command)
                .environment(environment)
                .runtime(is_runtime)
                .stopContainer(stop_on_finish)
                .deleteContainer(delete_on_finish)
                .build();
    }
}
