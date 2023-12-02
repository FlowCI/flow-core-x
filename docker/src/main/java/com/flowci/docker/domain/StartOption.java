package com.flowci.docker.domain;

import com.flowci.common.helper.StringHelper;
import com.flowci.domain.StringVars;
import io.fabric8.kubernetes.api.model.EnvVar;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class StartOption {

    private String image;

    private String name;

    private final StringVars env = new StringVars();

    public boolean hasName() {
        return StringHelper.hasValue(name);
    }

    public void addEnv(String k, String v) {
        this.env.put(k, v);
    }

    public List<String> toEnvList() {
        return env.toList();
    }

    public List<EnvVar> toK8sVarList() {
        List<EnvVar> list = new ArrayList<>(env.size());
        env.forEach((k, v) -> list.add(new EnvVar(k, v, null)));
        return list;
    }
}
