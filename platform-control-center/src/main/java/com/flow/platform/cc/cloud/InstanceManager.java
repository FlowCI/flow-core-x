package com.flow.platform.cc.cloud;

import com.flow.platform.util.mos.Instance;

import java.util.Collection;
import java.util.List;

/**
 * Created by gy@fir.im on 05/06/2017.
 * Copyright fir.im
 */
public interface InstanceManager {

    /**
     * Get running instance list
     *
     * @return
     */
    Collection<Instance> runningInstance();

    /**
     * Get failure instance which start with err
     *
     * @return
     */
    Collection<Instance> failureInstance();

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

    /**
     * Delete all instance and clean the cloud
     */
    void clean();
}
