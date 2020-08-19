package com.flowci.docker.domain;

import com.github.dockerjava.api.model.Frame;
import lombok.Getter;

@Getter
public class Output {

    private final byte[] data;

    public Output(Frame frame) {
        this.data = frame.getPayload();
    }

    public Output(String line) {
        this.data = line.getBytes();
    }

    public Output(byte[] data) {
        this.data = data;
    }
}
