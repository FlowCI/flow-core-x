package com.flowci.docker.domain;

import com.flowci.common.domain.StringVars;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class ContainerStartOption extends StartOption {

    private final List<String> entrypoint = new LinkedList<>();

    private final StringVars bind = new StringVars();

    public void addEntryPoint(String cmd) {
        this.entrypoint.add(cmd);
    }

    public void addBind(String src, String target) {
        this.bind.put(src, target);
    }

    public List<Bind> toBindList() {
        List<Bind> list = new ArrayList<>(bind.size());
        bind.forEach((s, t) -> list.add(new Bind(s, new Volume(t))));
        return list;
    }
}
