package com.flow.platform.util.mos;

import org.json.JSONObject;

/**
 * Created by gy@fir.im on 01/06/2017.
 * Copyright fir.im
 */
public class MosException extends RuntimeException {
    private JSONObject error;
    private Instance instance;

    public MosException(String message, Throwable e) {
        super(message, e);
    }

    public MosException(String message, Throwable e, Instance instance) {
        super(message, e);
        this.instance = instance;
    }

    public JSONObject getError() {
        return error;
    }

    public void setError(JSONObject error) {
        this.error = error;
    }

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }
}
