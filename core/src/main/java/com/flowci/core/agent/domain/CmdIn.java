package com.flowci.core.agent.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class CmdIn {

    public enum Type {
        /**
         * Execute shell script
         */
        SHELL,

        /**
         * Kill running shell
         */
        KILL,

        /**
         * Stream cmd
         */
        Stream,

        /**
         * Close agent
         */
        CLOSE;
    }

    protected Type type;

    protected CmdIn(Type type) {
        this.type = type;
    }
}
