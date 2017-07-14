package com.flow.platform.exception;

/**
 * Created by gy@fir.im on 06/07/2017.
 * Copyright fir.im
 */
public class IllegalParameterException extends FlowException {

    public IllegalParameterException(String description) {
        super(description, null);
    }
}
