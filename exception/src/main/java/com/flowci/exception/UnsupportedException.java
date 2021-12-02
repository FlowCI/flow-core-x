package com.flowci.exception;

public class UnsupportedException extends CIException {

    public UnsupportedException(String message, String... params) {
        super(message, params);
    }

    public UnsupportedException(String message, Throwable cause, String... params) {
        super(message, cause, params);
    }

    @Override
    public Integer getCode() {
        return ErrorCode.UNSUPPORTED;
    }
}
