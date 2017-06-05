package com.flow.platform.cc.pool;

import java.util.List;

/**
 * Created by gy@fir.im on 05/06/2017.
 * Copyright fir.im
 */
public interface PoolManager {

    /**
     * Async to start instance
     *
     * @param numOfInstance
     * @return List of instance name
     */
    List<String> batchStartInstance(final int numOfInstance);

    /**
     * Periodically check and delete failure instance
     */
    void deleteFailureInstance();
}
