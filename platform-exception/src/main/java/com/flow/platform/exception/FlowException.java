package com.flow.platform.exception;

/**
 * Created by gy@fir.im on 06/07/2017.
 * Copyright fir.im
 */
public abstract class FlowException extends RuntimeException {

    public FlowException(String description, Throwable e) {
        super(description, e);
    }
}
