package com.flowci.docker.domain;

import com.flowci.domain.StringVars;
import com.flowci.util.StringHelper;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import io.fabric8.kubernetes.api.model.EnvVar;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class DockerStartOption {

    private String image;

    private String name;

    private final List<String> entrypoint = new LinkedList<>();

    private final StringVars env = new StringVars();

    private final StringVars bind = new StringVars();

    public boolean hasName() {
        return StringHelper.hasValue(name);
    }

    public void addEntryPoint(String cmd) {
        this.entrypoint.add(cmd);
    }

    public void addEnv(String k, String v) {
        this.env.put(k, v);
    }

    public void addBind(String src, String target) {
        this.bind.put(src, target);
    }

    public List<Bind> toBindList() {
        List<Bind> list = new ArrayList<>(bind.size());
        bind.forEach((s, t) -> list.add(new Bind(s, new Volume(t))));
        return list;
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
