package com.flowci.sm;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Context {

    protected Status current;

    protected Status to;

    protected Throwable error;

    protected boolean skip;
}
