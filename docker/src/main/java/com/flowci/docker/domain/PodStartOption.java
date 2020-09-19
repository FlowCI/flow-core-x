package com.flowci.docker.domain;

import com.flowci.util.ObjectsHelper;
import com.flowci.util.StringHelper;
import io.fabric8.kubernetes.api.model.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class PodStartOption extends StartOption {

    private String command;

    private String label = "app";

    private List<String> args = new LinkedList<>();

    public PodBuilder buildPod(String labelKey, String labelVal) {
        return new PodBuilder()
                .withNewSpec()
                .withContainers(containerForImage(), defaultDockerContainer())
                .withRestartPolicy("Never")
                .endSpec()
                .withNewMetadata()
                .withName(getName())
                .addToLabels(labelKey, labelVal)
                .endMetadata();
    }

    private Container defaultDockerContainer() {
        return new ContainerBuilder()
                .withImage("docker:dind")
                .withImagePullPolicy("Always")
                .withName("docker-runtime")
                .withNewSecurityContext()
                .withPrivileged(true)
                .and()
                .withEnv(new EnvVar("DOCKER_TLS_CERTDIR", "", null))
                .build();
    }

    private Container containerForImage() {
        List<EnvVar> env = toK8sVarList();

        // set default vars for container
        Variables.PodVarsAndFieldPath.forEach((var, fieldPath) -> {
            env.add(new EnvVarBuilder()
                    .withName(var)
                    .withValueFrom(new EnvVarSource(null, new ObjectFieldSelector(null, fieldPath), null, null))
                    .build()
            );
        });

        // set docker host to docker-runtime container
        env.add(new EnvVar("DOCKER_HOST", "tcp://localhost:2375", null));

        ContainerBuilder builder = new ContainerBuilder()
                .withImage(getImage())
                .withImagePullPolicy("Always")
                .withName(getName())
                .withEnv(env);

        if (StringHelper.hasValue(command)) {
            builder.withCommand(command);
        }

        if (ObjectsHelper.hasCollection(args)) {
            builder.withArgs(args);
        }

        return builder.build();
    }
}
