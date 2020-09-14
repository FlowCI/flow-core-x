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

    public void addArg(String arg) {
        this.args.add(arg);
    }

    public PodBuilder buildPod(String labelKey, String labelVal) {
        return new PodBuilder()
                .withNewSpec()
                .withContainers(buildContainer().build())
                .withRestartPolicy("Never")
                .endSpec()
                .withNewMetadata()
                .withName(getName())
                .addToLabels(labelKey, labelVal)
                .endMetadata();
    }

    private ContainerBuilder buildContainer() {
        List<EnvVar> env = toK8sVarList();

        // set default vars for container
        Variables.PodVarsAndFieldPath.forEach((var, fieldPath) -> {
            env.add(new EnvVarBuilder()
                    .withName(var)
                    .withValueFrom(new EnvVarSource(null, new ObjectFieldSelector(null, fieldPath), null, null))
                    .build()
            );
        });

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

        return builder;
    }
}
