package com.flowci.core.job.manager;

import com.flowci.zookeeper.InterLock;
import com.flowci.zookeeper.ZookeeperClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Log4j2
@Component
public class LockManagerImpl implements LockManager {

    private static final int DefaultJobLockTimeout = 20; // seconds

    @Autowired
    private ZookeeperClient zk;

    @Override
    public Optional<InterLock> lock(String jobId) {
        String path = zk.makePath("/job-locks", jobId);
        Optional<InterLock> lock = zk.lock(path, DefaultJobLockTimeout);
        lock.ifPresent(interLock -> log.debug("Lock: {}", jobId));
        return lock;
    }

    @Override
    public void unlock(InterLock lock, String jobId) {
        try {
            zk.release(lock);
            log.debug("Unlock: {}", jobId);
        } catch (Exception warn) {
            log.warn(warn);
        }
    }
}
