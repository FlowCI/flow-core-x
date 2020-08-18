package com.flowci.docker.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class PodStartOption extends StartOption {

    private String command;

    private List<String> args = new LinkedList<>();

    public void addArg(String arg) {
        this.args.add(arg);
    }
}
