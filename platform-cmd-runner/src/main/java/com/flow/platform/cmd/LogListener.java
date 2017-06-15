package com.flow.platform.cmd;

/**
 * Created by gy@fir.im on 21/05/2017.
 * Copyright fir.im
 */
public interface LogListener {

    void onLog(Log log);

    void onFinish();
}
