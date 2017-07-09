package com.flow.platform.util.zk;

/**
 * Created by gy@fir.im on 09/07/2017.
 * Copyright fir.im
 */
public abstract class AbstractZkException extends RuntimeException {
    private Exception raw;

    public AbstractZkException(Exception raw, String message) {
        super(message);
        this.raw = raw;
    }

    public Exception getRaw() {
        return raw;
    }
}
