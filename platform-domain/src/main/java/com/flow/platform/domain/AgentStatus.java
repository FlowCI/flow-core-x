package com.flow.platform.domain;

import java.io.Serializable;

/**
 * Created by gy@fir.im on 09/06/2017.
 * Copyright fir.im
 */
public enum AgentStatus implements Serializable {

    OFFLINE("OFFLINE"),

    IDLE("IDLE"),

    BUSY("BUSY");

    private String name;

    AgentStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
