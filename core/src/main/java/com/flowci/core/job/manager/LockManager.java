package com.flowci.core.job.manager;

import com.flowci.zookeeper.InterLock;

import java.util.Optional;

public interface LockManager {

    Optional<InterLock> lock(String jobId);

    void unlock(InterLock lock, String jobId);
}
