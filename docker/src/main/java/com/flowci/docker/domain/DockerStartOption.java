package com.flowci.docker.domain;

import com.flowci.util.StringHelper;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class DockerStartOption {

    private String image;

    private String name;

    private final Map<String, String> env = new HashMap<>();

    private final Map<String, String> bind = new HashMap<>();

    public boolean hasName() {
        return StringHelper.hasValue(name);
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
        List<String> list = new ArrayList<>(env.size());
        env.forEach((k, v) -> list.add(String.format("%s=%s", k, v)));
        return list;
    }
}
