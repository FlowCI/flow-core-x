package com.flowci.sm;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Context {

    private Status current;

    private Status to;

    private Throwable error;

}
