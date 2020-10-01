package com.flowci.core.agent.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author yang
 */
public abstract class TtyCmd {

    public enum Action {
        OPEN,

        CLOSE,

        SHELL
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public final static class In extends CmdIn {

        private String id;

        private Action action;

        private String input;

        public In() {
            super(Type.TTY);
        }
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public final static class Out implements CmdOut {

        private String id;

        private Action action;

        private boolean success;

        private String error;
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public final static class Log {

        private String id;

        private String log;
    }
}
