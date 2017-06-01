package com.flow.platform.util.mos;

/**
 * Created by gy@fir.im on 01/06/2017.
 * Copyright fir.im
 */
public class MosError {

    public static class CreateInstanceException extends RuntimeException {
        public CreateInstanceException(String message) {
            super(message);
        }
    }
}
